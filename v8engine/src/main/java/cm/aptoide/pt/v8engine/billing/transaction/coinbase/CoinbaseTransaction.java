package cm.aptoide.pt.v8engine.billing.transaction.coinbase;

import android.util.Log;

import com.coinbase.api.Coinbase;
import com.coinbase.api.CoinbaseBuilder;
import com.coinbase.api.entity.Transaction;

import org.joda.money.Money;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.v8engine.billing.transaction.BitcoinTransactionService;
import cm.aptoide.pt.v8engine.billing.transaction.LocalTransaction;
import cm.aptoide.pt.v8engine.billing.view.bitcoin.TransactionSimulator;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by jose_messejana on 31-07-2017.
 */

public class CoinbaseTransaction extends LocalTransaction {

    private final String localMetadata;
    private TransactionSimulator transactionSimulator;
    private com.coinbase.api.entity.Transaction cbtransaction;

    public CoinbaseTransaction(String productId, String payerId, Status status, int paymentMethodId,
                               String localMetadata, String payload, String sellerId) {
        super(productId, payerId, status, paymentMethodId, localMetadata, payload, sellerId);
        this.localMetadata = localMetadata;
        Logger.e("teste3",localMetadata);
        if(localMetadata != null) {
            Logger.e("teste3","transaction");
            Coinbase coinbaseInstance = new CoinbaseBuilder().withAccessToken(localMetadata).build();
            if (!BitcoinTransactionService.REALTRANSACTION) {
                try {
                    coinbaseInstance.getUser().getEmail();
                    transactionSimulator = new TransactionSimulator();
                    Single.just(true).delay(TransactionSimulator.TIME_FOR_TEST_TRANSACTION, TimeUnit.MILLISECONDS)
                            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess(__ -> transactionSimulator.startThread())
                            .subscribe(__ -> Logger.e("teste3",transactionSimulator.getStatus().toString()),throwable -> throwable.printStackTrace());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                NumberFormat formatter = new DecimalFormat("#");
                formatter.setMaximumFractionDigits(8);
                Double p = (0.0000001);
                String preco = formatter.format(p);
                try {
                    // if (!coinbaseInstance.getUser().getBalance().minus(p).isNegative()) {
                    com.coinbase.api.entity.Transaction coinbasetransaction = new com.coinbase.api.entity.Transaction();
                    coinbasetransaction.setTo(BitcoinTransactionService.EMAIL); //mail da coinbase ou bitcoin address
                    coinbasetransaction.setAmount(Money.parse("BTC " + preco));
                    coinbaseInstance.sendMoney(coinbasetransaction);
                    cbtransaction = coinbasetransaction;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        }

    public String getLocalMetadata(){ return localMetadata;
    }

    public boolean isCompleted() {
        Logger.e("teste3","iscompleted");
        if (!BitcoinTransactionService.REALTRANSACTION) {
            return TransactionSimulator.Estado.COMPLETED.equals(transactionSimulator.getStatus());
        }
        else {

            return Transaction.DetailedStatus.COMPLETED.equals(cbtransaction.getDetailedStatus());
        }
    }

    public boolean isPending() {
        Logger.e("teste3","isPending");
        if(!BitcoinTransactionService.REALTRANSACTION) {
            boolean equals = TransactionSimulator.Estado.PENDING.equals(transactionSimulator.getStatus());
            Log.d("teste3", "isPending() returned: " + equals);
            return equals;
        }
        else{
            boolean b = Transaction.DetailedStatus.PENDING.equals(cbtransaction.getDetailedStatus())
                    || Transaction.DetailedStatus.WAITING_FOR_CLEARING.equals(cbtransaction.getDetailedStatus())
                    || Transaction.DetailedStatus.WAITING_FOR_SIGNATURE.equals(cbtransaction.getDetailedStatus());
            Log.d("teste3", "isPending() returned real : " + b);
            return b;
        }
    }

    public boolean isFailed() {
        Logger.e("teste3","isFailed");
        if(!BitcoinTransactionService.REALTRANSACTION) {
            return TransactionSimulator.Estado.CANCELED.equals(transactionSimulator.getStatus())
                    || TransactionSimulator.Estado.FAILED.equals(transactionSimulator.getStatus());
        }
        else{
            return Transaction.DetailedStatus.FAILED.equals(cbtransaction.getDetailedStatus())
                    || Transaction.DetailedStatus.CANCELED.equals(cbtransaction.getDetailedStatus())
                    || Transaction.DetailedStatus.EXPIRED.equals(cbtransaction.getDetailedStatus());
        }
    }

    public boolean isUnknown() {
        Logger.e("teste3","isUnknown");
        if(!BitcoinTransactionService.REALTRANSACTION) {
            return TransactionSimulator.Estado.OTHER.equals(transactionSimulator.getStatus())
                    || (!isFailed() && !isPending() && !isCompleted());
        }
        else{
            return !isPending() && !isCompleted() && !isFailed();
        }
    }


    public TransactionSimulator getTransactionSimulator(){ return transactionSimulator;
    }


    public com.coinbase.api.entity.Transaction getCBTransaction(){ return cbtransaction;
    }
}
