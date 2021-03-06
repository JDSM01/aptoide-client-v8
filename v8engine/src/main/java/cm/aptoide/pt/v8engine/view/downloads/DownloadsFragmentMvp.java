package cm.aptoide.pt.v8engine.view.downloads;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v7.BodyInterceptor;
import cm.aptoide.pt.networkclient.WebService;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.v8engine.InstallManager;
import cm.aptoide.pt.v8engine.R;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.analytics.Analytics;
import cm.aptoide.pt.v8engine.download.DownloadEventConverter;
import cm.aptoide.pt.v8engine.download.InstallEventConverter;
import cm.aptoide.pt.v8engine.install.InstallerFactory;
import cm.aptoide.pt.v8engine.presenter.DownloadsPresenter;
import cm.aptoide.pt.v8engine.presenter.DownloadsView;
import cm.aptoide.pt.v8engine.repository.RepositoryFactory;
import cm.aptoide.pt.v8engine.view.custom.DividerItemDecoration;
import cm.aptoide.pt.v8engine.view.fragment.FragmentView;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Converter;

public class DownloadsFragmentMvp extends FragmentView implements DownloadsView {

  private DownloadsAdapter adapter;
  private View noDownloadsView;
  private InstallEventConverter installConverter;
  private DownloadEventConverter downloadConverter;
  private InstallManager installManager;
  private Analytics analytics;

  public static DownloadsFragmentMvp newInstance() {
    return new DownloadsFragmentMvp();
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final OkHttpClient httpClient =
        ((V8Engine) getContext().getApplicationContext()).getDefaultClient();
    final BodyInterceptor<BaseBody> baseBodyInterceptorV7 =
        ((V8Engine) getContext().getApplicationContext()).getBaseBodyInterceptorV7();
    final Converter.Factory converterFactory = WebService.getDefaultConverter();
    installConverter =
        new InstallEventConverter(baseBodyInterceptorV7, httpClient, converterFactory);
    downloadConverter =
        new DownloadEventConverter(baseBodyInterceptorV7, httpClient, converterFactory);
    installManager = ((V8Engine) getContext().getApplicationContext()).getInstallManager(
        InstallerFactory.ROLLBACK);
    analytics = Analytics.getInstance();
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.recycler_fragment_downloads, container, false);

    RecyclerView downloadsRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    downloadsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

    final int pixelDimen = AptoideUtils.ScreenU.getPixelsForDip(5);
    final DividerItemDecoration decor =
        new DividerItemDecoration(pixelDimen, DividerItemDecoration.ALL);
    downloadsRecyclerView.addItemDecoration(decor);

    adapter = new DownloadsAdapter(installConverter, downloadConverter, installManager, analytics);
    downloadsRecyclerView.setAdapter(adapter);
    noDownloadsView = view.findViewById(R.id.no_apps_downloaded);

    attachPresenter(
        new DownloadsPresenter(this, RepositoryFactory.getDownloadRepository(), installManager),
        savedInstanceState);

    return view;
  }

  @UiThread @Override public void showActiveDownloads(List<Download> downloads) {
    setEmptyDownloadVisible(false);
    adapter.setActiveDownloads(downloads);
  }

  @UiThread @Override public void showStandByDownloads(List<Download> downloads) {
    setEmptyDownloadVisible(false);
    adapter.setStandByDownloads(downloads);
  }

  @UiThread @Override public void showCompletedDownloads(List<Download> downloads) {
    setEmptyDownloadVisible(false);
    adapter.setCompletedDownloads(downloads);
  }

  @UiThread @Override public void showEmptyDownloadList() {
    setEmptyDownloadVisible(true);
    adapter.clearAll();
  }

  @UiThread private void setEmptyDownloadVisible(boolean visible) {
    if (noDownloadsView.getVisibility() == View.GONE && visible) {
      noDownloadsView.setVisibility(View.VISIBLE);
    }

    if (noDownloadsView.getVisibility() == View.VISIBLE && !visible) {
      noDownloadsView.setVisibility(View.GONE);
    }
  }
}
