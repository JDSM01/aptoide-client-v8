package cm.aptoide.pt.v8engine.billing.view;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.jakewharton.rxrelay.PublishRelay;

import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.billing.PaymentMethodMapper;
import cm.aptoide.pt.v8engine.billing.authorization.coinbase.CoinbaseOAuth;
import cm.aptoide.pt.v8engine.view.permission.PermissionServiceFragment;
import cm.aptoide.pt.v8engine.view.rx.RxAlertDialog;
import rx.Observable;

public class WebViewFragment extends PermissionServiceFragment
    implements cm.aptoide.pt.v8engine.billing.view.WebView {

  private WebView webView;
  private View progressBarContainer;
  private RxAlertDialog unknownErrorDialog;
  private PublishRelay<Void> mainUrlSubject;
  private PublishRelay<Void> redirectUrlSubject;
  private PublishRelay<Void> backButtonSelectionSubject;
  private ClickHandler clickHandler;
  private ProgressDialog progressDialog;
  private int paymenMethod;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mainUrlSubject = PublishRelay.create();
    redirectUrlSubject = PublishRelay.create();
    backButtonSelectionSubject = PublishRelay.create();
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_payment_web_view, container, false);
  }

  @SuppressLint("SetJavaScriptEnabled") @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    webView = (WebView) view.findViewById(R.id.activity_boa_compra_authorization_web_view);
    webView.getSettings()
        .setJavaScriptEnabled(true);
    webView.setWebChromeClient(new WebChromeClient());
    progressBarContainer = view.findViewById(R.id.activity_web_authorization_preogress_bar);

    unknownErrorDialog =
        new RxAlertDialog.Builder(getContext()).setMessage(R.string.all_message_general_error)
            .setPositiveButton(R.string.ok)
            .build();
    clickHandler = () -> {
      backButtonSelectionSubject.call(null);
      return false;
    };
    registerClickHandler(clickHandler);
  }

  @Override public void onDestroyView() {
    ((ViewGroup) webView.getParent()).removeView(webView);
    webView.setWebViewClient(null);
    webView.destroy();
    webView = null;
    progressDialog.dismiss();
    progressDialog = null;
    unknownErrorDialog.dismiss();
    unknownErrorDialog = null;
    unregisterClickHandler(clickHandler);
    clickHandler = null;
    progressBarContainer = null;
    super.onDestroyView();
  }

  public void showLoading() {
    progressBarContainer.setVisibility(View.VISIBLE);
  }

  public void hideLoading() {
    progressBarContainer.setVisibility(View.GONE);
  }

  public void loadWebsite(String mainUrl, String redirectUrl) {
    webView.setWebViewClient(new WebViewClient() {

      @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (url.equals(redirectUrl)) {
          redirectUrlSubject.call(null);
        }
      }

      @Override public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mainUrlSubject.call(null);
      }
    });
    webView.loadUrl(mainUrl);
  }


  public void loadWebsitewithContainingRedirect(String mainUrl, String redirectUrl) {
    webView.setWebViewClient(new WebViewClient() {

      @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (url.contains(redirectUrl)) {
          CoinbaseOAuth.cbredirectUrl = url;
          showProgressBar();
          redirectUrlSubject.call(null);

        }
      }

      @Override public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mainUrlSubject.call(null);
      }
    });
      webView.loadUrl(mainUrl);
  }

  public void showProgressBar(){
    if(PaymentMethodMapper.BITCOIN == paymenMethod) {
      String text = "Wating to confirm transaction, please wait";
      progressDialog = new ProgressDialog(getActivity());
      progressDialog.setTitle("Transaction Status");
      progressDialog.setMessage(text);
      progressDialog.show();
    }
  }

  public void showCompleteToast(String state){
    Toast.makeText(getActivity(), "Transaction "+state,
            Toast.LENGTH_SHORT).show();
  }

  public void setPaymentMethod(int paymentMethod){
    this.paymenMethod = paymentMethod;
  }
  /* public void showConfirmationDialog(double price){
    alertDialog = new ProgressDialog.Builder(getActivity()).create();
    String text = "Confirm that you want to send Bitcoins";
    alertDialog.setTitle("Sending           "+price+" BTC");
    alertDialog.setMessage(text);
   /* alertDialog.setButton(1, "OK", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        CoinbasePresenter.confirmation = OK;
      }
    }); :
    alertDialog.setButton(0, "CANCEL", new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        CoinbasePresenter.confirmation = CANCEL;
      }
    });
    alertDialog.show();
  } */



  public Observable<Void> redirectUrlEvent() {
    return redirectUrlSubject;
  }

  public Observable<Void> backButtonEvent() {
    return backButtonSelectionSubject;
  }

  public Observable<Void> urlLoadedEvent() {
    return mainUrlSubject;
  }

  public void showError() {
    unknownErrorDialog.show();
  }

  public Observable<Void> errorDismissedEvent() {
    return unknownErrorDialog.dismisses()
        .map(dialogInterface -> null);
  }
}
