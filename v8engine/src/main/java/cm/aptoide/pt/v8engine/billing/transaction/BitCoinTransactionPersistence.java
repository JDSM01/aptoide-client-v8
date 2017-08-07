package cm.aptoide.pt.v8engine.billing.transaction;

import cm.aptoide.pt.database.accessors.Database;
import cm.aptoide.pt.database.realm.PaymentConfirmation;
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
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId,
                                                 String productId, Transaction.Status status, String payload, String metadata) {
        return service.createTransactionwithstatus( productId,  metadata, status,  payerId,  paymentMethodId );
    }

    @Override
    public Observable<Transaction> getTransaction(String sellerId, String payerId, String productId) {
        if(service.getTransaction(sellerId,productId, payerId) == null) {
            return realm.getRealm()
                    .map(realm -> realm.where(PaymentConfirmation.class)
                            .equalTo(PaymentConfirmation.PRODUCT_ID, productId)
                            .equalTo(PaymentConfirmation.PAYER_ID, payerId))
                    .flatMap(query -> realm.findAsList(query))
                    .flatMap(paymentConfirmations -> Observable.from(paymentConfirmations)
                            .map(paymentConfirmation -> mapper.map(paymentConfirmation))
                            .defaultIfEmpty(
                                    factory.create(sellerId, payerId, -1,productId,Transaction.Status.NEW, null, null,
                                            null, null, null)));
        }
        return Observable.just(service.getTransaction(sellerId, productId,payerId));
    }

    @Override
    public Completable removeTransaction(String sellerId, String payerId, String productId) {
        return service.removeTransaction(productId);
    }

    @Override
    public Completable saveTransaction(Transaction transaction) {
        return service.saveTransaction(transaction);
    }
}
