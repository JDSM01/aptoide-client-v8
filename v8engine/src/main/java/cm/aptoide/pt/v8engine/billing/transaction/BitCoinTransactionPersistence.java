package cm.aptoide.pt.v8engine.billing.transaction;

import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Created by jose_messejana on 04-08-2017.
 */

public class BitCoinTransactionPersistence implements TransactionPersistence {

    private final BitcoinTransactionService service;

    public BitCoinTransactionPersistence(BitcoinTransactionService service) {
        this.service = service;
    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId,
                                                 String productId, Transaction.Status status, String payload, String metadata) {
        return service.createTransactionwithstatus( productId,  metadata, status,  payerId,  paymentMethodId );
    }

    @Override
    public Observable<Transaction> getTransaction(String sellerId, String payerId, String productId) {
        if(service.getTransaction(sellerId,productId, payerId) == null) {
            service.createTransactionwithstatus(productId,null,Transaction.Status.NEW, payerId, -1);
            return Observable.just(new Transaction(productId,payerId,Transaction.Status.NEW,-1, null, sellerId));
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
