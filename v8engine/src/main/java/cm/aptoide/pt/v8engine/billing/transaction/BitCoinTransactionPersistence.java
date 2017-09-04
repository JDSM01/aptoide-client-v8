package cm.aptoide.pt.v8engine.billing.transaction;

import com.jakewharton.rxrelay.PublishRelay;

import java.util.HashMap;
import java.util.Map;

import cm.aptoide.pt.logger.Logger;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Created by jose_messejana on 04-08-2017.
 */

public class BitCoinTransactionPersistence implements TransactionPersistence {

    private Map<String, Transaction> transactionList = new HashMap<>();
    private final PublishRelay<Transaction> transactionRelay;
    private BitcoinTransactionService bitcoinTransactionService;
    private final TransactionFactory transactionFactory;

    public BitCoinTransactionPersistence(Map<String, Transaction> transactionList,
                                         PublishRelay<Transaction> transactionRelay,
                                         BitcoinTransactionService bitcoinTransactionService, TransactionFactory transactionFactory) {
        this.transactionList = transactionList;
        this.transactionRelay = transactionRelay;
        this.bitcoinTransactionService = bitcoinTransactionService;
        this.transactionFactory = transactionFactory;
    }

    @Override
    public Single<Transaction> createTransaction(String sellerId, String payerId, int paymentMethodId,
                                                 String productId, Transaction.Status status, String payload, String metadata) {
        return bitcoinTransactionService.createTransactionwithMeta(sellerId,payerId, paymentMethodId, productId, status
                ,metadata);

    }

    @Override
    public Observable<Transaction> getTransaction(String sellerId, String payerId, String productId) {
        return transactionRelay;
    }

    @Override
    public Completable removeTransaction(String sellerId, String payerId, String productId) {
       return Completable.fromAction(() -> {
           transactionList.remove(productId+payerId);
       });
    }

    @Override
    public Completable saveTransaction(Transaction transaction) {
        Logger.e("teste3","persistence"+transaction.getStatus().toString());
        return Completable.fromAction(() -> {
            transactionList.put((transaction.getProductId() + transaction.getPayerId()), transaction);
            transactionRelay.call(transaction);
        });
    }
}
