package cm.aptoide.pt.v8engine.billing.view.bitcoin;

import cm.aptoide.pt.spotandshare.socket.Log;

/**
 * Created by jose_messejana on 11-08-2017.
 */

public class TransactionSimulator {
    private Estado status;
    private static final int TIME_FOR_TEST_TRANSACTION = 10000; //10s


    public TransactionSimulator(){
        Thread t = new Thread();
        status = Estado.PENDING;
        try {
            t.sleep(TIME_FOR_TEST_TRANSACTION);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("teste3","here");
        status = Estado.COMPLETE;
    }

    public Estado getStatus(){
        return status;
    }

    public enum Estado{
        PENDING,COMPLETE,OTHER,FAILED,CANCELED;
    }
}
