package cm.aptoide.pt.v8engine.billing.transaction;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v3.BaseBody;
import cm.aptoide.pt.v8engine.billing.PaymentMethodMapper;
import cm.aptoide.pt.v8engine.billing.Product;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Single;

/**
 * Created by jose_messejana on 27-07-2017.
 */

public class BitcoinTransactionService implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final BodyInterceptor<BaseBody> bodyInterceptorV3;
    private final Converter.Factory converterFactory;
    private final OkHttpClient httpClient;
    private final TokenInvalidator tokenInvalidator;
    private final SharedPreferences sharedPreferences;
    private final TransactionFactory transactionFactory;
    private Transaction transaction = null;
    private Map<String, Transaction> transactionList = new HashMap<>();

    public BitcoinTransactionService(TransactionMapper transactionMapper,
                                     BodyInterceptor<BaseBody> bodyInterceptorV3, Converter.Factory converterFactory,
                                     OkHttpClient httpClient, TokenInvalidator tokenInvalidator,
                                     SharedPreferences sharedPreferences, TransactionFactory transactionFactory
                                     ) {
        this.transactionMapper = transactionMapper;
        this.bodyInterceptorV3 = bodyInterceptorV3;
        this.converterFactory = converterFactory;
        this.httpClient = httpClient;
        this.tokenInvalidator = tokenInvalidator;
        this.sharedPreferences = sharedPreferences;
        this.transactionFactory = transactionFactory;
    }
    @Override
    public Single<Transaction> getTransaction(String sellerId, String payerId, Product product) {
        return Single.just(transactionList.get(concat(product.getId(),payerId)));
    }

    public Transaction getTransaction(String sellerId, String productid, String payerId){
        return (transactionList.get(concat(productid, payerId)));
    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId, Product product,
                                                 String metadata) { //not applicable
        return null;
    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId, Product product,
                                                 String metadata, String payload) {
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            transaction = transactionFactory.create(null, payerId,paymentMethodId, product.getId(),
                    Transaction.Status.PENDING_USER_AUTHORIZATION, null, null, null, null,null);
            saveTransaction(transaction);
            return Single.just(transaction);

        }
        return null;

    }


    public Single<Transaction> createTransactionCompleted(String productid, int paymentMethodId, String payerId) {
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            transaction = transactionFactory.create(null, payerId, paymentMethodId, productid,
                    Transaction.Status.COMPLETED, null, null, null, null, null);
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
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
                transaction = transactionFactory.create(null, payerId, paymentMethodId, productId, status, null, null, null, null,null);
            saveTransaction(transaction);
            return Single.just(transaction);
        }
        return null;
    }


    public Completable removeTransaction(String productId) {
        transaction = null;
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
}

