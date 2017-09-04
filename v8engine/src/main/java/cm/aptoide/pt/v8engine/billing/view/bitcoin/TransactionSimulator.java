package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import cm.aptoide.pt.logger.Logger;

/**
 * Created by jose_messejana on 11-08-2017.
 */

public class TransactionSimulator{
    private Estado status;
    public static final int TIME_FOR_TEST_TRANSACTION = 10000; //10s


    public TransactionSimulator(){
        status = Estado.PENDING;
    }

    public Estado   getStatus(){
        return status;
    }

    public void startThread(){
        Logger.e("teste3",TIME_FOR_TEST_TRANSACTION+"sec passed");
        status = Estado.COMPLETED;
            }

    public enum Estado{
        PENDING,COMPLETED,OTHER,FAILED,CANCELED;
    }
}
