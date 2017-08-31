package cm.aptoide.pt.v8engine.billing.transaction;

import android.content.SharedPreferences;

import com.coinbase.api.Coinbase;

import java.util.HashMap;
import java.util.Map;

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

/**
 * Created by jose_messejana on 27-07-2017.
 */

public class BitcoinTransactionService implements TransactionService {

    public static final boolean REALTRANSACTION = false;
    private static final String EMAIL = "Jose.Messejana@aptoide.com";
    private final TransactionFactory transactionFactory;
    private Transaction transaction = null;
    private Map<String, Transaction> transactionList = new HashMap<>();
    private Map<String, TransactionSimulator> coinbaseTransactionList = new HashMap<>();
    private Map<String, com.coinbase.api.entity.Transaction> coinbaseCBTransactionList = new HashMap<>();
    private Coinbase coinbaseInstance;
    private Product product;

    public BitcoinTransactionService(TransactionMapper transactionMapper,
                                     BodyInterceptor<BaseBody> bodyInterceptorV3, Converter.Factory converterFactory,
                                     OkHttpClient httpClient, TokenInvalidator tokenInvalidator,
                                     SharedPreferences sharedPreferences, TransactionFactory transactionFactory
                                     ) {

        this.transactionFactory = transactionFactory;
    }
    @Override
    public Single<Transaction> getTransaction(String sellerId, String payerId, Product product) {
        Logger.e("teste3","here@");
        return Single.just(transactionList.get(concat(product.getId(),payerId)));
    }

    public Transaction getTransaction(String sellerId, String productid, String payerId){
        Logger.e("teste3","herenot@");
      /*  Transaction transaction = transactionList.get(concat(productid,payerId));
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
            } */
            return transactionList.get(concat(productid, payerId));
//        }
//        else{
//            return transaction;
//        }
    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId, Product product,
                                                 String payload) {
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            this.product = product;
            transaction = transactionFactory.create(null, payerId,paymentMethodId, product.getId(),
                    Transaction.Status.PENDING_USER_AUTHORIZATION, null, null, null, null,null);
            return Single.just(transaction);
        }
        return null;

    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId, Product product,
                                                 String metadata, String payload) {
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            transaction = transactionFactory.create(null, payerId,paymentMethodId, product.getId(),
                    Transaction.Status.PENDING_USER_AUTHORIZATION, null, null, null, null,null);
            return Single.just(transaction);

        }
        return null;

    }


    public Single<Transaction> createTransactionStatusUpdate(String sellerId, String productid, int paymentMethodId, String payerId, Transaction.Status status) { //made
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            transaction = transactionFactory.create(sellerId, payerId, paymentMethodId,productid, status, null, null, null, null,null);
            saveTransaction(transaction);
            return Single.just(transaction);

        }
        return null;

    }

    public Transaction getTransaction(){
        return transaction;
    }

    /////////////////

    public Single<Transaction> createTransactionwithstatus(String productId, String metadata, Transaction.Status status, String payerId, int paymentMethodId) {
        //Logger.e("teste3","createTransaction1");
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
         /*       coinbaseInstance = new CoinbaseBuilder().withAccessToken(metadata).build();
            if(!REALTRANSACTION) {
                try {
                    String email = coinbaseInstance.getUser().getEmail();
                    TransactionSimulator bitcoinTransactionSimulatoraux = new TransactionSimulator();
                    addTStransaction(productId, payerId, bitcoinTransactionSimulatoraux);
                    Single.just(true).delay(TransactionSimulator.TIME_FOR_TEST_TRANSACTION, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSuccess(__ ->bitcoinTransactionSimulatoraux.startThread()).subscribe();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else{
                if(BitcoinTransactionService.REALTRANSACTION) {
                    NumberFormat formatter = new DecimalFormat("#");
                    formatter.setMaximumFractionDigits(8);
                    Double p = (0.0000001);
                    String preco = formatter.format(p);
                        try {
                            // if (!coinbaseInstance.getUser().getBalance().minus(p).isNegative()) {
                            com.coinbase.api.entity.Transaction coinbasetransaction = new com.coinbase.api.entity.Transaction();
                            coinbasetransaction.setTo(EMAIL); //mail da coinbase ou bitcoin address
                            coinbasetransaction.setAmount(Money.parse("BTC " + preco));
                            addCBtransaction(productId,payerId,coinbaseInstance.sendMoney(coinbasetransaction));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } */
            transaction = transactionFactory.create(null, payerId, paymentMethodId, productId, status, metadata, null, null, null,null);
            saveTransaction(transaction);
            return Single.just(transaction);
            }
        return null;
    }


    public Completable removeTransaction(String productId) {
        String payer = transaction.getPayerId();
        transaction = null;
        transactionList.remove(concat(productId,payer));
        return Completable.complete();
    }


    public Completable removeAllTransactions() {
        transactionList.clear();
        transaction = null;
        return Completable.complete();
    }

    public Completable saveTransaction(Transaction transaction) {
        transactionList.put(concat(transaction.getProductId(),transaction.getPayerId()), transaction);
        return Completable.complete();
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

    public com.coinbase.api.entity.Transaction getCBtransaction(String productID, String payerID){
        return coinbaseCBTransactionList.get(concat(productID,payerID));
    }

    public void addCBtransaction(String productID, String payerID, com.coinbase.api.entity.Transaction transaction){
        coinbaseCBTransactionList.put(concat(productID,payerID),transaction);
    }

    public Product getProduct(){
        if(product.getId() == transaction.getProductId())
            return product;
        else{
            return null;
        }
    }
}

