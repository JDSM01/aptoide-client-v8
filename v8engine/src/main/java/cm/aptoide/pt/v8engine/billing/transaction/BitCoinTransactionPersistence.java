package cm.aptoide.pt.v8engine.billing.transaction;

import android.content.SharedPreferences;
import android.util.Log;

import cm.aptoide.pt.database.accessors.Database;
import cm.aptoide.pt.database.realm.PaymentConfirmation;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v3.BaseBody;
import cm.aptoide.pt.v8engine.billing.PaymentMethodMapper;
import io.realm.Realm;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Created by jose_messejana on 04-08-2017.
 */

public class BitCoinTransactionPersistence implements TransactionPersistence {

    private final BitcoinTransactionService service;
    private final Database realm;
    private final TransactionMapper mapper;
    private final TransactionFactory factory;

    public BitCoinTransactionPersistence(BitcoinTransactionService service, Database realm, TransactionMapper mapper, TransactionFactory factory) {
        this.service = service;
        this.realm = realm;
        this.mapper = mapper;
        this.factory = factory;
    }

    @Override
    public Single<Transaction> createTransaction(int productId, String metadata, Transaction.Status status, String payerId, int paymentMethodId) {
        return service.createTransactionwithstatus( productId,  metadata, status,  payerId,  paymentMethodId );
    }

    @Override
    public Observable<Transaction> getTransaction(int productId, String payerId) {
        if(service.getTransaction(productId, payerId) == null) {
            return realm.getRealm()
                    .map(realm -> realm.where(PaymentConfirmation.class)
                            .equalTo(PaymentConfirmation.PRODUCT_ID, productId)
                            .equalTo(PaymentConfirmation.PAYER_ID, payerId))
                    .flatMap(query -> realm.findAsList(query))
                    .flatMap(paymentConfirmations -> Observable.from(paymentConfirmations)
                            .map(paymentConfirmation -> mapper.map(paymentConfirmation))
                            .defaultIfEmpty(
                                    factory.create(productId, payerId, Transaction.Status.NEW, -1, null,
                                            null, null, null)));
        }
        return Observable.just(service.getTransaction(productId,payerId));
    }

    @Override
    public Completable removeTransaction(int productId) {
        return service.removeTransaction(productId);
    }

    @Override
    public Completable removeAllTransactions() {
        return service.removeAllTransactions();
    }

    @Override
    public Completable saveTransaction(Transaction transaction) {
        return service.saveTransaction(transaction);
    }
}
