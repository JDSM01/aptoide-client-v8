package cm.aptoide.pt.downloadmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cm.aptoide.pt.logger.Logger;

/**
 * Created by trinkes on 6/23/16.
 */
public class NotificationEventReceiver extends BroadcastReceiver {

	private static final String TAG = NotificationEventReceiver.class.getSimpleName();

	public void onReceive(Intent intent) {
		Logger.d(TAG, "onReceive() called with: " + "intent = [" + intent + "]");

		String action = intent.getAction();
		if (action != null) {
			AptoideDownloadManager downloadManager = AptoideDownloadManager.getInstance();
			switch (action) {
				case AptoideDownloadManager.DOWNLOADMANAGER_ACTION_PAUSE:
					if (intent.hasExtra(AptoideDownloadManager.APP_ID_EXTRA)) {
						long appid = intent.getLongExtra(AptoideDownloadManager.APP_ID_EXTRA, -1);
						if (appid > 0) {
							downloadManager.pauseDownload(appid);
						} else {
							downloadManager.pauseAllDownloads();
						}
					}
				case AptoideDownloadManager.DOWNLOADMANAGER_ACTION_OPEN:
					if (downloadManager.getDownloadNotificationActionsInterface() != null) {
						downloadManager.getDownloadNotificationActionsInterface()
								.button1Pressed();
					}
					break;
				case AptoideDownloadManager.DOWNLOADMANAGER_ACTION_START_DOWNLOAD:
					if (intent.hasExtra(AptoideDownloadManager.APP_ID_EXTRA)) {
						long appid = intent.getLongExtra(AptoideDownloadManager.APP_ID_EXTRA, -1);
						if (appid > 0) {
							downloadManager.getDownload(appid).first().flatMap(downloadManager::startDownload).subscribe(
									download -> Logger.d("DownloadManager", "Download of " + download.getAppName() + " resumed"),
									throwable -> Logger.e("DownloadManager", "Failed to resume download: " + throwable.getMessage()));
						}
					}
					break;
				case AptoideDownloadManager.DOWNLOADMANAGER_ACTION_NOTIFICATION:
					if (downloadManager.getDownloadNotificationActionsInterface() != null) {
						if (intent.hasExtra(AptoideDownloadManager.APP_ID_EXTRA)) {
							downloadManager.getDownloadNotificationActionsInterface()
									.notificationPressed(intent.getLongExtra(AptoideDownloadManager.APP_ID_EXTRA, 0));
						}
						break;
					}
			}
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		onReceive(intent);
	}
}
