package cm.aptoide.pt.v8engine.view.app.widget;

import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import cm.aptoide.pt.imageloader.ImageLoader;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.model.v7.Malware;
import cm.aptoide.pt.model.v7.listapp.App;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.crashreports.CrashReport;
import cm.aptoide.pt.v8engine.view.app.OtherVersionDisplayable;
import cm.aptoide.pt.v8engine.view.recycler.widget.Displayables;
import cm.aptoide.pt.v8engine.view.recycler.widget.Widget;
import java.util.Locale;

@Displayables({ OtherVersionDisplayable.class }) public class OtherVersionWidget
    extends Widget<OtherVersionDisplayable> implements View.OnClickListener {

  private static final String TAG = OtherVersionWidget.class.getSimpleName();
  private static final Locale DEFAULT_LOCALE = Locale.getDefault();
  private static final AptoideUtils.DateTimeU DATE_TIME_U = AptoideUtils.DateTimeU.getInstance();

  // left side
  //private ImageView versionBadge;
  private TextView version;
  private ImageView trustedBadge;
  private TextView date;
  private TextView downloads;
  // right side
  private ImageView storeIcon;
  private TextView storeNameView;
  private TextView followers;

  private long appId;
  private String packageName;
  private String storeName;

  public OtherVersionWidget(View itemView) {
    super(itemView);
  }

  @Override protected void assignViews(View itemView) {
    // left side
    //versionBadge = (ImageView) itemView.findViewById(R.id.version_icon);
    version = (TextView) itemView.findViewById(R.id.version_name);
    trustedBadge = (ImageView) itemView.findViewById(R.id.badge_icon);
    date = (TextView) itemView.findViewById(R.id.version_date);
    downloads = (TextView) itemView.findViewById(R.id.downloads);
    // right side
    storeIcon = (ImageView) itemView.findViewById(R.id.store_icon);
    storeNameView = (TextView) itemView.findViewById(R.id.store_name);
    followers = (TextView) itemView.findViewById(R.id.store_followers);

    itemView.setOnClickListener(this);
  }

  @Override public void bindView(OtherVersionDisplayable displayable) {
    setItemBackgroundColor(itemView);
    try {
      final App app = displayable.getPojo();
      appId = app.getId();
      storeName = app.getStore()
          .getName();
      packageName = app.getPackageName();

      version.setText(app.getFile()
          .getVername());
      setBadge(app);
      date.setText(DATE_TIME_U.getTimeDiffString(getContext(), app.getModified()
          .getTime()));
      downloads.setText(String.format(DEFAULT_LOCALE,
          getContext().getString(R.string.other_versions_downloads_count_text),
          AptoideUtils.StringU.withSuffix(app.getStats()
              .getDownloads())));

      ImageLoader.with(getContext())
          .load(app.getStore()
              .getAvatar(), storeIcon);
      storeNameView.setText(app.getStore()
          .getName());
      followers.setText(String.format(DEFAULT_LOCALE,
          getContext().getString(R.string.appview_followers_count_text), app.getStore()
              .getStats()
              .getSubscribers()));
    } catch (NullPointerException e) {
      CrashReport.getInstance()
          .log(e);
    }
  }

  private void setItemBackgroundColor(View itemView) {
    final Resources.Theme theme = itemView.getContext()
        .getTheme();
    final Resources res = itemView.getResources();

    int color;
    if (getLayoutPosition() % 2 == 0) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        color = res.getColor(R.color.light_custom_gray, theme);
      } else {
        color = res.getColor(R.color.light_custom_gray);
      }
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        color = res.getColor(R.color.white, theme);
      } else {
        color = res.getColor(R.color.white);
      }
    }

    itemView.setBackgroundColor(color);
  }

  private void setBadge(App app) {
    @DrawableRes int badgeResId;

    Malware.Rank rank = app.getFile()
        .getMalware()
        .getRank() == null ? Malware.Rank.UNKNOWN : app.getFile()
        .getMalware()
        .getRank();
    switch (rank) {
      case TRUSTED:
        badgeResId = R.drawable.ic_badge_trusted;
        break;

      case WARNING:
        badgeResId = R.drawable.ic_badge_warning;
        break;

      case CRITICAL:
        badgeResId = R.drawable.ic_badge_critical;
        break;

      default:
      case UNKNOWN:
        badgeResId = 0;
        break;
    }
    // keep the remaining compound drawables in TextView and set the one on the right
    // Drawable[] drawables = version.getCompoundDrawables();
    //version.setCompoundDrawables(drawables[0], drawables[1], ImageLoader.load(badgeResId), drawables[3]);
    // does not work properly because "The Drawables must already have had setBounds(Rect) called". info from:
    // https://developer.android.com/reference/android/widget/TextView.html#setCompoundDrawables
    trustedBadge.setImageResource(badgeResId);
  }

  @Override public void onClick(View v) {
    Logger.d(TAG, "showing other version for app with id = " + appId);
    getFragmentNavigator().navigateTo(V8Engine.getFragmentProvider()
        .newAppViewFragment(appId, packageName, null, storeName));
  }
}
