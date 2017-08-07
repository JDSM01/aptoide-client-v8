package cm.aptoide.pt.v8engine.billing.transaction.coinbase;

import cm.aptoide.pt.v8engine.billing.transaction.Transaction;

/**
 * Created by jose_messejana on 31-07-2017.
 */

public class CoinbaseTransaction extends Transaction {

    private final String confirmationUrl;
    private final String successUrl;

    public CoinbaseTransaction(String productId, String payerId, Status status, int paymentMethodId,
                          String confirmationUrl, String successUrl) {
        super(productId, payerId, status, paymentMethodId, null, null);
        this.confirmationUrl = confirmationUrl;
        this.successUrl = successUrl;
    }

    public String getConfirmationUrl() {
        return confirmationUrl;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

}
