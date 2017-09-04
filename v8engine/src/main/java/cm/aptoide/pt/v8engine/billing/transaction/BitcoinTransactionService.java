package cm.aptoide.pt.v8engine.billing.transaction;

import android.content.SharedPreferences;

import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;

import org.joda.money.Money;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v3.BaseBody;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.v8engine.billing.PaymentMethodMapper;
import cm.aptoide.pt.v8engine.billing.Product;
import cm.aptoide.pt.v8engine.billing.view.bitcoin.TransactionSimulator;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by jose_messejana on 27-07-2017.
 */

public class BitcoinTransactionService implements TransactionService {

    public static final boolean REALTRANSACTION = false;
    public static final String EMAIL = "Jose.Messejana@aptoide.com";
    private final TransactionFactory transactionFactory;
    private Transaction currentTransaction;
    private Map<String, Transaction> transactionList = new HashMap<>();
    private Map<String, TransactionSimulator> coinbaseTransactionList = new HashMap<>();
    private Map<String, String> coinbaseCBTransactionList = new HashMap<>();

    public BitcoinTransactionService(TransactionMapper transactionMapper,
                                     BodyInterceptor<BaseBody> bodyInterceptorV3, Converter.Factory converterFactory,
                                     OkHttpClient httpClient, TokenInvalidator tokenInvalidator,
                                     SharedPreferences sharedPreferences, TransactionFactory transactionFactory
                                     ) {

        this.transactionFactory = transactionFactory;
    }
    @Override
    public Single<Transaction> getTransaction(String sellerId, String payerId, Product product) {
        Transaction transaction = transactionList.get(concat(product.getId(), payerId));
        if (transaction != null && (!coinbaseTransactionList.isEmpty() || !coinbaseCBTransactionList.isEmpty())) {
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
        else{
            return createTransactionwithstatus(product.getId(),null,Transaction.Status.NEW, payerId, -1);
        }
        return Single.just(transaction);
    }


  /*  public Transaction getTransaction(String sellerId, String productid, String payerId){
        Logger.e("teste3","herenot@");
        Transaction transaction = transactionList.get(concat(productid,payerId));
        if(transaction != null) {
            if (!REALTRANSACTION) {
                TransactionSimulator transactionSimulator = coinbaseTransactionList.get(concat(productid, payerId));
                if(transactionSimulator != null) {
                    switch (transactionSimulator.getStatus(
)) {
                        case COMPLETED:
                            createTransactionStatusUpdate(sellerId, productid, transaction.getPaymentMethodId(), payerId,
                                    cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.COMPLETED);
                            break;
                        case FAILED:
                            createTransactionStatusUpdate(sellerId, productid, transaction.getPaymentMethodId(), payerId,
                                    cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.FAILED);
                            break;
                        case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
                            createTransactionStatusUpdate(sellerId, productid, transaction.getPaymentMethodId(), payerId,
                                    cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.CANCELED);
                            break;
                        default:
                    }
                }
            } else {
                com.coinbase.api.entity.Transaction cbtransaction = coinbaseCBTransactionList.get(concat(productid, payerId));
                if(cbtransaction != null) {
                    switch (cbtransaction.getDetailedStatus()) {
                        case COMPLETED:
                            createTransactionStatusUpdate(sellerId, productid, transaction.getPaymentMethodId(), payerId,
                                    cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.COMPLETED);
                            break;
                        case FAILED:
                            createTransactionStatusUpdate(sellerId, productid, transaction.getPaymentMethodId(), payerId,
                                    cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.FAILED);
                            break;
                        case CANCELED: //BitCoin Transactions cannot be canceled, however the SDK has a CANCELED STATUS so it's better to handle it
                            createTransactionStatusUpdate(sellerId, productid, transaction.getPaymentMethodId(), payerId,
                                    cm.aptoide.pt.v8engine.billing.transaction.Transaction.Status.CANCELED);
                            break;
                        default:
                    }
                }
            }
            return transactionList.get(concat(productid, payerId));
        }
       else{
            return transaction;
        }
    } */

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId, Product product,
                                                 String payload) {
        Transaction transaction = null;
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            transaction = transactionFactory.create(null, payerId,paymentMethodId, product.getId(),
                    Transaction.Status.PENDING_USER_AUTHORIZATION, null, null, null, null,null);
            return saveTransaction(transaction).andThen(Single.just(transaction));
        }
        return Single.just(transaction);

    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId, Product product,
                                                 String metadata, String payload) {
        Logger.e("teste3","herenot");
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there

        }
        return null;
    }

    public Single<Transaction> createTransactionwithMeta(String sellerId, String payerId, int paymentMethodId, String productId,
                                                         Transaction.Status status, String metadata) {
        Transaction transaction = null;
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            try {
                Coinbase coinbaseInstance = new CoinbaseBuilder().withAccessToken(metadata).build();
                if (!REALTRANSACTION) {
                    String s = coinbaseInstance.getUser().getEmail();
                    TransactionSimulator transactionSimulator = new TransactionSimulator();
                    Single.just(true).delay(TransactionSimulator.TIME_FOR_TEST_TRANSACTION, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess(__ -> transactionSimulator.startThread())
                            .subscribe(__ -> Logger.e("teste3",transactionSimulator.getStatus().toString()),throwable -> throwable.printStackTrace());
                    addTStransaction(productId,payerId,transactionSimulator);
                }
                else{
                    NumberFormat formatter = new DecimalFormat("#");
                    formatter.setMaximumFractionDigits(8);
                    Double p = (0.0000001);
                    String preco = formatter.format(p);
                    // if (!coinbaseInstance.getUser().getBalance().minus(p).isNegative()) {
                    com.coinbase.api.entity.Transaction coinbasetransaction = new com.coinbase.api.entity.Transaction();
                    coinbasetransaction.setTo(BitcoinTransactionService.EMAIL); //mail da coinbase ou bitcoin address
                    coinbasetransaction.setAmount(Money.parse("BTC " + preco));
                    coinbaseInstance.sendMoney(coinbasetransaction);
                    String hash = coinbasetransaction.getHash();
                    addCBtransaction(productId,payerId,hash);
                }
                transaction = transactionFactory.create(sellerId, payerId, paymentMethodId,productId, status, metadata, null, null, null,null);
            }catch(Exception e){ e.printStackTrace();}
        }
        if(transaction != null) {
            saveTransaction(transaction).andThen(Single.just(transaction));
        }
        else{
            transaction = transactionFactory.create(sellerId, payerId, paymentMethodId,productId, Transaction.Status.FAILED,
                    metadata, null, null, null,null);
            saveTransaction(transaction).andThen(Single.just(transaction));
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

    /////////////////

    public Single<Transaction> createTransactionwithstatus(String productId, String metadata, Transaction.Status status, String payerId, int paymentMethodId) {
            Transaction transaction = transactionFactory.create(null, payerId, paymentMethodId, productId, status, metadata, null, null, null,null);
            currentTransaction = transaction;
            return saveTransaction(transaction).andThen(Single.just(transaction));
    }



    public Completable removeAllTransactions() {
        transactionList.clear();
        return Completable.complete();
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

    ///////// Coinbase Simulator transactions

    public TransactionSimulator getTStransaction(String productID, String payerID){
        return coinbaseTransactionList.get(concat(productID,payerID));
    }

    public void addTStransaction(String productID, String payerID, TransactionSimulator transaction){
        coinbaseTransactionList.put(concat(productID,payerID),transaction);
    }

    //////////Real Transaction/////////

    public String getCBtransaction(String productID, String payerID){
        return coinbaseCBTransactionList.get(concat(productID,payerID));
    }

    public void addCBtransaction(String productID, String payerID, String hash){
        coinbaseCBTransactionList.put(concat(productID,payerID),hash);
    }

}

