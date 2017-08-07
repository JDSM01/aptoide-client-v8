package cm.aptoide.pt.v8engine.billing.transaction;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cm.aptoide.pt.database.accessors.Database;
import cm.aptoide.pt.database.realm.PaymentConfirmation;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.model.v3.ErrorResponse;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v3.BaseBody;
import cm.aptoide.pt.v8engine.billing.PaymentMethodMapper;
import cm.aptoide.pt.v8engine.billing.Product;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Observable;
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
    public Single<Transaction> getTransaction(Product product, String payerId) {
        return Single.just(transactionList.get(concat(product.getId(),payerId)));
    }

    public Transaction getTransaction(int productid, String payerId){
        return (transactionList.get(concat(productid, payerId)));
    }

    @Override
    public Single<Transaction> createTransaction(Product product, int paymentMethodId, String payerId,
                                                 String metadata) { //not applicable
        return null;
    }

    @Override
    public Single<Transaction> createTransaction(Product product, int paymentMethodId, String payerId) {
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            transaction = transactionFactory.create(product.getId(), payerId,
                    Transaction.Status.PENDING_USER_AUTHORIZATION, paymentMethodId, null, null, null, null);
            saveTransaction(transaction);
            return Single.just(transaction);

        }
        return null;

    }


    public Single<Transaction> createTransactionCompleted(int productid, int paymentMethodId, String payerId) {
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
            transaction = transactionFactory.create(productid, payerId,
                    Transaction.Status.COMPLETED, paymentMethodId, null, null, null, null);
            saveTransaction(transaction);
            return Single.just(transaction);

        }
        return null;

    }

    public Transaction getTransaction(){
        return transaction;
    }

    /////////////////

    public Single<Transaction> createTransactionwithstatus(int productId, String metadata, Transaction.Status status, String payerId, int paymentMethodId) {
        if (paymentMethodId == PaymentMethodMapper.BITCOIN) { //may need to check if OAuth is already there
                transaction = transactionFactory.create(productId, payerId, status, paymentMethodId, null, null, null, null);
            saveTransaction(transaction);
            return Single.just(transaction);
        }
        return null;
    }


    public Completable removeTransaction(int productId) {
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

    private String concat(int productId, String payerId){
        return productId+payerId;
    }
}

