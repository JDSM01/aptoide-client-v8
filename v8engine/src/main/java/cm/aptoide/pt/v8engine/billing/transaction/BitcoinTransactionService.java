package cm.aptoide.pt.v8engine.billing.transaction;

import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;

import org.joda.money.Money;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cm.aptoide.pt.v8engine.billing.PaymentMethodMapper;
import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.view.bitcoin.TransactionSimulator;
import rx.Completable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by jose_messejana on 27-07-2017.
 */

public class BitcoinTransactionService implements TransactionService {

    public static final boolean REALTRANSACTION = false;
    public static final String EMAIL = "";
    private final TransactionFactory transactionFactory;
    private Transaction currentTransaction;
    private Map<String, Transaction> transactionList = new HashMap<>();
    private Map<String, TransactionSimulator> coinbaseTransactionList = new HashMap<>();
    private Map<String, String> coinbaseCBTransactionList = new HashMap<>();
    private Map<String, Product> products = new HashMap<>();

    public BitcoinTransactionService(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    @Override
    public Single<Transaction> getTransaction(String sellerId, String payerId, Product product) {
        Transaction transaction = transactionList.get(concat(product.getId(), payerId));
        if (transaction != null){
            if(!coinbaseTransactionList.isEmpty() || !coinbaseCBTransactionList.isEmpty()) {
                if (transaction.getPaymentMethodId() == PaymentMethodMapper.BITCOIN) {
                    if (!REALTRANSACTION) {
                        TransactionSimulator transactionSimulator = coinbaseTransactionList.get(concat(product.getId(), payerId));
                        if (transactionSimulator != null) {
                            switch (transactionSimulator.getStatus()) {
                                case COMPLETED:
                                    return createTransactionStatusUpdate(sellerId, product.getId(), transaction.getPaymentMethodId(), payerId,
                                            Transaction.Status.COMPLETED);
                                case FAILED:
                                    return createTransactionStatusUpdate(sellerId, product.getId(), transaction.getPaymentMethodId(), payerId,
                                            Transaction.Status.FAILED);
                                case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
                                    return createTransactionStatusUpdate(sellerId, product.getId(), transaction.getPaymentMethodId(), payerId,
                                            Transaction.Status.CANCELED);
                                case PENDING:
                                    break;
                                default:
                                    return createTransactionStatusUpdate(sellerId, product.getId(), transaction.getPaymentMethodId(), payerId,
                                            Transaction.Status.UNKNOWN);
                            }
                        }
                    } else {
                        String hash = coinbaseCBTransactionList.get(concat(product.getId(), payerId));
                        if (hash != null) {
                            LocalTransaction localTransaction = (LocalTransaction) transaction;
                            Coinbase coinbaseInstance = new CoinbaseBuilder().withAccessToken(localTransaction.getLocalMetadata()).build();
                            try {
                                com.coinbase.api.entity.Transaction cbTransaction = coinbaseInstance.getTransaction(hash);
                                switch (cbTransaction.getDetailedStatus()) {
                                    case COMPLETED:
                                        return createTransactionStatusUpdate(sellerId, product.getId(), transaction.getPaymentMethodId(), payerId,
                                                Transaction.Status.COMPLETED);
                                    case FAILED:
                                        return createTransactionStatusUpdate(sellerId, product.getId(), transaction.getPaymentMethodId(), payerId,
                                                Transaction.Status.FAILED);
                                    case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
                                        return createTransactionStatusUpdate(sellerId, product.getId(), transaction.getPaymentMethodId(), payerId,
                                                Transaction.Status.CANCELED);
                                    case WAITING_FOR_CLEARING:
                                        break;
                                    case WAITING_FOR_SIGNATURE:
                                        break;
                                    default:
                                        return createTransactionStatusUpdate(sellerId, product.getId(), transaction.getPaymentMethodId(), payerId,
                                                Transaction.Status.UNKNOWN);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        else{
            return createTransactionwithstatus(product.getId(),null,Transaction.Status.NEW, payerId, -1);
        }
        return Single.just(transaction);
    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId, Product product,
                                                 String payload) {
        Transaction transaction = null;
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) {
            products.put(product.getId(),product);
            transaction = transactionFactory.create(null, payerId,paymentMethodId, product.getId(),
                    Transaction.Status.PENDING_USER_AUTHORIZATION, null, null, null, null,null);
            saveTransaction(transaction);
        }
        return Single.just(transaction);

    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId, Product product,
                                                 String metadata, String payload) {
        return null;
    }

    public Single<Transaction> createTransactionwithMeta(String sellerId, String payerId, int paymentMethodId, String productId,
                                                         Transaction.Status status, String metadata){
        Transaction transaction = null;
        try {
            if (paymentMethodId == PaymentMethodMapper.BITCOIN) {
                Coinbase coinbaseInstance = new CoinbaseBuilder().withAccessToken(metadata).build();
                if (!REALTRANSACTION) {
                    coinbaseInstance.getUser().getEmail();
                    TransactionSimulator transactionSimulator = new TransactionSimulator();
                    Single.just(true).delay(TransactionSimulator.TIME_FOR_TEST_TRANSACTION, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess(__ -> transactionSimulator.startThread())
                            .subscribe(__ -> {
                            }, throwable -> throwable.printStackTrace());
                    addTStransaction(productId, payerId, transactionSimulator);
                } else {
                    String preco = Double.toString(products.get(productId).getPrice().getAmount());
                    String currency = products.get(productId).getPrice().getCurrency();
                    com.coinbase.api.entity.Transaction coinbasetransaction = new com.coinbase.api.entity.Transaction();
                    coinbasetransaction.setTo(BitcoinTransactionService.EMAIL); //mail da coinbase ou bitcoin address
                    coinbasetransaction.setAmount(Money.parse(currency + preco));
                    coinbaseInstance.sendMoney(coinbasetransaction);
                    String hash = coinbasetransaction.getHash();
                    addCBtransaction(productId, payerId, hash);
                }
                transaction = transactionFactory.create(sellerId, payerId, paymentMethodId, productId, status, metadata, null, null, null, null);
            }
            saveTransaction(transaction);
        }catch(Exception e){
            transaction = transactionFactory.create(sellerId, payerId, paymentMethodId,productId, Transaction.Status.FAILED,
                    metadata, null, null, null,null);
            saveTransaction(transaction);
        }
        return Single.just(transaction);
    }


    public Single<Transaction> createTransactionStatusUpdate(String sellerId, String productid, int paymentMethodId, String payerId, Transaction.Status status) { //made
        Transaction transaction = null;
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) {
            transaction = transactionFactory.create(sellerId, payerId, paymentMethodId,productid, status, null, null, null, null,null);
            saveTransaction(transaction).andThen(Single.just(transaction));
        }
        return Single.just(transaction);

    }

    public Single<Transaction> createTransactionwithstatus(String productId, String metadata, Transaction.Status status, String payerId, int paymentMethodId) {
            Transaction transaction = transactionFactory.create(null, payerId, paymentMethodId, productId, status, metadata, null, null, null,null);
            currentTransaction = transaction;
            return saveTransaction(transaction).andThen(Single.just(transaction));
    }


    public Completable saveTransaction(Transaction transaction) {
        transactionList.put(concat(transaction.getProductId(),transaction.getPayerId()), transaction);
        return Completable.complete();
    }

    public Transaction getCurrentTransaction(){
        return currentTransaction;
    }

    private String concat(String productId, String payerId){
        return productId+payerId;
    }


    public void addTStransaction(String productID, String payerID, TransactionSimulator transaction){
        coinbaseTransactionList.put(concat(productID,payerID),transaction);
    }

    public void addCBtransaction(String productID, String payerID, String hash){
        coinbaseCBTransactionList.put(concat(productID,payerID),hash);
    }

    public void remove(String key) {
        transactionList.remove(key);
    }
}

