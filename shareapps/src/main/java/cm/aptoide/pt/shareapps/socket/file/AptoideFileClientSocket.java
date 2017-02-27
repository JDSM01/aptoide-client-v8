package cm.aptoide.pt.shareapps.socket.file;

import cm.aptoide.pt.shareapps.socket.AptoideClientSocket;
import cm.aptoide.pt.shareapps.socket.entities.FileInfo;
import cm.aptoide.pt.shareapps.socket.interfaces.FileClientLifecycle;
import cm.aptoide.pt.shareapps.socket.interfaces.ProgressAccumulator;
import cm.aptoide.pt.shareapps.socket.util.MultiProgressAccumulator;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import static cm.aptoide.pt.shareapps.socket.util.FileInfoUtils.computeTotalSize;

/**
 * Created by neuro on 27-01-2017.
 */

public class AptoideFileClientSocket<T> extends AptoideClientSocket {

  private final List<FileInfo> fileInfos;

  private T fileDescriptor;
  private FileClientLifecycle<T> fileClientLifecycle;

  public AptoideFileClientSocket(String host, int port, List<FileInfo> fileInfos) {
    super(host, port);
    this.fileInfos = fileInfos;
    onError = fileClientLifecycle;
  }

  public AptoideFileClientSocket(int bufferSize, String host, int port, List<FileInfo> fileInfos) {
    super(bufferSize, host, port);
    this.fileInfos = fileInfos;
    onError = fileClientLifecycle;
  }

  @Override protected void onConnected(Socket socket) throws IOException {

    if (fileClientLifecycle != null) {
      fileClientLifecycle.onStartReceiving(fileDescriptor);
    }

    ProgressAccumulator progressAccumulator =
        new MultiProgressAccumulator(computeTotalSize(fileInfos), fileClientLifecycle);

    for (FileInfo fileInfo : fileInfos) {
      System.out.println(Thread.currentThread().getId() + ": Start receiving " + fileInfo);
      OutputStream out = new FileOutputStream(fileInfo.getFilePath());

      copy(socket.getInputStream(), out, fileInfo.getSize(), progressAccumulator);
      out.close();
    }

    if (fileClientLifecycle != null) {
      fileClientLifecycle.onFinishReceiving(fileDescriptor);
    }
  }

  public AptoideFileClientSocket<T> setFileClientLifecycle(T fileDescriptor,
      FileClientLifecycle<T> fileClientLifecycle) {

    if (fileDescriptor == null) {
      throw new IllegalArgumentException("fileDescriptor cannot be null!");
    }

    if (fileClientLifecycle == null) {
      throw new IllegalArgumentException("fileClientLifecycle cannot be null!");
    }

    this.fileDescriptor = fileDescriptor;
    this.fileClientLifecycle = fileClientLifecycle;

    return this;
  }
}