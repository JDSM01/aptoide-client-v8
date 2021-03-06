package cm.aptoide.pt.v8engine.view.app.displayable;

import cm.aptoide.pt.model.v7.GetApp;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.app.AppViewAnalytics;

public class AppViewStoreDisplayable extends AppViewDisplayable {

  public AppViewStoreDisplayable() {
  }

  public AppViewStoreDisplayable(GetApp getApp, AppViewAnalytics appViewAnalytics) {
    super(getApp, appViewAnalytics);
  }

  @Override protected Configs getConfig() {
    return new Configs(1, true);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_app_view_subscription;
  }
}
