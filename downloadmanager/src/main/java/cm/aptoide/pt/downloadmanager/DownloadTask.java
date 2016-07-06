package cm.aptoide.pt.downloadmanager;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.util.concurrent.TimeUnit;

import cm.aptoide.pt.database.Database;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.FileToDownload;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.utils.FileUtils;
import io.realm.Realm;
import lombok.Cleanup;
import lombok.Setter;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

/**
 * Created by trinkes on 5/13/16.
 */
public class DownloadTask extends FileDownloadLargeFileListener {

	public static final int INTERVAL = 1000;    //interval between progress updates
	public static final int APTOIDE_DOWNLOAD_TASK_TAG_KEY = 888;
	private static final String TAG = DownloadTask.class.getSimpleName();

	final Download download;
	private final long appId;
	/**
	 * this boolean is used to change between serial and parallel download (in this downloadTask) the default value is
	 * true
	 */
	@Setter boolean isSerial = true;
	private ConnectableObservable<Download> observable;

	public DownloadTask(Download download) {
		this.download = download;
		this.appId = download.getAppId();

		this.observable = Observable.interval(INTERVAL / 4, INTERVAL, TimeUnit.MILLISECONDS)
				.subscribeOn(Schedulers.io())
				.map(aLong -> updateProgress())
				.filter(updatedDownload -> {
					if (updatedDownload.getOverallProgress() <= AptoideDownloadManager.PROGRESS_MAX_VALUE && download
							.getOverallDownloadStatus() == Download.PROGRESS) {
						if (updatedDownload.getOverallProgress() == AptoideDownloadManager.PROGRESS_MAX_VALUE && download.getOverallDownloadStatus() !=
								Download.COMPLETED) {
							setDownloadStatus(Download.COMPLETED, download);
							AptoideDownloadManager.getInstance().currentDownloadFinished(download.getAppId());
						}
						return true;
					} else {
						return false;
					}
				})
				//				.takeUntil(integer1 -> download.getOverallDownloadStatus() != Download.PROGRESS)
				.publish();
		observable.connect();
	}

	@NonNull
	static String getFilePathFromFileType(FileToDownload fileToDownload) {
		String path;
		switch (fileToDownload.getFileType()) {
			case FileToDownload.APK:
				path = AptoideDownloadManager.APK_PATH;
				break;
			case FileToDownload.OBB:
				path = AptoideDownloadManager.OBB_PATH + fileToDownload.getPackageName();
				break;
			case FileToDownload.GENERIC:
			default:
				path = AptoideDownloadManager.GENERIC_PATH;
				break;
		}
		return path;
	}


	/**
	 * Update the overall download progress. It updates the value on database and in memory list
	 *
	 * @return new current progress
	 */
	@NonNull
	public Download updateProgress() {
		if (download.getOverallProgress() >= AptoideDownloadManager.PROGRESS_MAX_VALUE || download.getOverallDownloadStatus() != Download
				.PROGRESS) {
			return download;
		}

		int progress = 0;
		for (final FileToDownload fileToDownload : download.getFilesToDownload()) {
			progress += fileToDownload.getProgress();
		}
		download.setOverallProgress((int) Math.floor((float) progress / download.getFilesToDownload().size()));
		saveDownloadInDb(download);
		return download;
	}

	/**
	 * @throws IllegalArgumentException
	 */
	public void startDownload() throws IllegalArgumentException {
		if (download.getFilesToDownload() != null) {
			for (FileToDownload fileToDownload : download.getFilesToDownload()) {
				if (TextUtils.isEmpty(fileToDownload.getLink())) {
					throw new IllegalArgumentException("A link to download must be provided");
				}
				BaseDownloadTask baseDownloadTask = FileDownloader.getImpl().create(fileToDownload.getLink());
				baseDownloadTask.setTag(APTOIDE_DOWNLOAD_TASK_TAG_KEY, this);
				fileToDownload.setDownloadId(baseDownloadTask.setListener(this).setCallbackProgressTimes(AptoideDownloadManager.PROGRESS_MAX_VALUE)
						.setPath(AptoideDownloadManager.DOWNLOADS_STORAGE_PATH + fileToDownload.getFileName())
						.ready());
				fileToDownload.setAppId(appId);
			}

			if (isSerial) {
				// To form a queue with the same queueTarget and execute them linearly
				Logger.d(TAG, "startDownload() called with: " + "1");
				FileDownloader.getImpl().start(this, true);
			} else {
				// To form a queue with the same queueTarget and execute them in parallel
				Logger.d(TAG, "startDownload() called with: " + "2");
				FileDownloader.getImpl().start(this, false);
			}
		}
		saveDownloadInDb(download);
	}


	private synchronized void saveDownloadInDb(Download download) {
		Observable.fromCallable(() -> {
			Logger.d(TAG, "saveDownloadInDb() called with: " + "download = [" + download.getAppId() + "," + download.getStatusName(AptoideDownloadManager
					.getContext()) +
					"]");
			@Cleanup Realm realm = Database.get();
			Database.save(download, realm);
			return null;
		}).subscribeOn(Schedulers.io()).subscribe();
	}

	public Observable<Download> getObservable() {
		return observable;
	}

	@Override
	protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
		pending(task, (long) soFarBytes, (long) totalBytes);
		setDownloadStatus(Download.PENDING, download, task);
	}

	@Override
	protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
		progress(task, (long) soFarBytes, (long) totalBytes);
	}

	@Override
	protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
		paused(task, (long) soFarBytes, (long) totalBytes);
	}

	@Override
	protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
		setDownloadStatus(Download.PENDING, download, task);
	}

	@Override
	protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
		for (FileToDownload fileToDownload : download.getFilesToDownload()) {
			if (fileToDownload.getDownloadId() == task.getId()) {
				fileToDownload.setProgress((int) Math.floor((float) soFarBytes / totalBytes * AptoideDownloadManager.PROGRESS_MAX_VALUE));
			}
		}
		if (download.getOverallDownloadStatus() != Download.PROGRESS) {
			setDownloadStatus(Download.PROGRESS, download, task);
			AptoideDownloadManager.getInstance().setDownloading(true);
		}
	}

	@Override
	protected void blockComplete(BaseDownloadTask task) {
		Logger.d(TAG, "blockComplete() called with: " + "task = [" + task + "]");
	}

	@Override
	protected void completed(BaseDownloadTask task) {
		for (FileToDownload fileToDownload : download.getFilesToDownload()) {
			if (fileToDownload.getDownloadId() == task.getId()) {
				fileToDownload.setPath(getFilePathFromFileType(fileToDownload));
				fileToDownload.setStatus(Download.COMPLETED);
				moveFileToRightPlace(download);
				fileToDownload.setProgress(AptoideDownloadManager.PROGRESS_MAX_VALUE);
			}
		}
		saveDownloadInDb(download);
		AptoideDownloadManager.getInstance().setDownloading(false);
	}

	@Override
	protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
		setDownloadStatus(Download.PAUSED, download, task);
		AptoideDownloadManager.getInstance().currentDownloadFinished(download.getAppId());
	}

	@Override
	protected void error(BaseDownloadTask task, Throwable e) {
		Logger.printException(e);
		AptoideDownloadManager.getInstance().pauseDownload(download.getAppId());
		setDownloadStatus(Download.ERROR, download, task);
	}

	@Override
	protected void warn(BaseDownloadTask task) {
		setDownloadStatus(Download.WARN, download, task);
	}


	private void setDownloadStatus(@Download.DownloadState int status, Download download) {
		setDownloadStatus(status, download, null);
	}

	private void setDownloadStatus(@Download.DownloadState int status, Download download, @Nullable BaseDownloadTask
			task) {
		if (task != null) {
			for (final FileToDownload fileToDownload : download.getFilesToDownload()) {
				if (fileToDownload.getDownloadId() == task.getId()) {
					fileToDownload.setStatus(status);
				}
			}
		}

		this.download.setOverallDownloadStatus(status);
		saveDownloadInDb(download);
		if (status == Download.PROGRESS) {
			AptoideDownloadManager.getInstance().setDownloading(true);
		} else {
			AptoideDownloadManager.getInstance().setDownloading(false);
		}
	}

	private void moveFileToRightPlace(Download download) {
		for (final FileToDownload fileToDownload : download.getFilesToDownload()) {
			if (fileToDownload.getStatus() != Download.COMPLETED) {
				return;
			}
		}

		for (final FileToDownload fileToDownload : download.getFilesToDownload()) {
			FileUtils.copyFile(AptoideDownloadManager.DOWNLOADS_STORAGE_PATH, fileToDownload.getPath(), fileToDownload.getFileName())
					.subscribeOn(Schedulers.io())
					.subscribe(copiedSuccessful -> {
						if (!copiedSuccessful) {
							setDownloadStatus(Download.ERROR, download);
						}
					});
		}
	}
}
