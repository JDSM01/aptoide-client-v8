package cm.aptoide.pt.v8engine.billing.transaction;

import cm.aptoide.pt.v8engine.billing.PaymentMethodMapper;
import cm.aptoide.pt.v8engine.billing.transaction.braintree.BraintreeTransaction;
import cm.aptoide.pt.v8engine.billing.transaction.coinbase.CoinbaseTransaction;
import cm.aptoide.pt.v8engine.billing.transaction.mol.MolTransaction;

public class TransactionFactory {

  public Transaction create(int productId, String payerId, Transaction.Status status,
      int paymentMethodId, String metadata, String confirmationUrl, String successUrl,
      String clientToken) {
    switch (paymentMethodId) {
      case PaymentMethodMapper.PAYPAL:
      case PaymentMethodMapper.BRAINTREE_CREDIT_CARD:
        if (clientToken == null) {
          return new LocalTransaction(productId, payerId, status, paymentMethodId, metadata);
        }
        return new BraintreeTransaction(productId, payerId, status, paymentMethodId, clientToken);
      case PaymentMethodMapper.MOL_POINTS:
        return new MolTransaction(productId, payerId, status, paymentMethodId, confirmationUrl,
            successUrl);
      case PaymentMethodMapper.BOA_COMPRA:
      case PaymentMethodMapper.BITCOIN:
        return new CoinbaseTransaction(productId, payerId, status, paymentMethodId, "http://www.yourhtmlsource.com/myfirstsite/",
                "http://www.yourhtmlsource.com/starthere/");
      case PaymentMethodMapper.BOA_COMPRA_GOLD:
      case PaymentMethodMapper.SANDBOX:
      default:
        return new Transaction(productId, payerId, status, paymentMethodId);
    }
  }
}