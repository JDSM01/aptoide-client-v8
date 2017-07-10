package cm.aptoide.pt.spotandshareapp.view;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import cm.aptoide.pt.spotandshare.socket.entities.AndroidAppInfo;
import cm.aptoide.pt.spotandshareapp.R;
import java.util.List;
import rx.subjects.PublishSubject;

/**
 * Created by filipe on 05-07-2017.
 */

public class SpotAndShareTransferRecordAdapter
    extends RecyclerView.Adapter<SpotAndShareTransferRecordAdapter.TransferViewHolder> {

  private List<AndroidAppInfo> appsTransfered;
  private PublishSubject<AndroidAppInfo> acceptSubject;

  public SpotAndShareTransferRecordAdapter(List<AndroidAppInfo> appsTransfered,
      PublishSubject<AndroidAppInfo> acceptSubject) {
    this.appsTransfered = appsTransfered;
    this.acceptSubject = acceptSubject;
  }

  @Override public TransferViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.fragment_spotandshare_app_selection_item, parent, false);
    return new TransferViewHolder(view);
  }

  @Override public void onBindViewHolder(TransferViewHolder holder, int position) {
    holder.setTransferItem(appsTransfered.get(position));
  }

  @Override public int getItemCount() {
    return appsTransfered.size();
  }

  class TransferViewHolder extends ViewHolder {

    private TextView senderName;
    private ImageView appIcon;
    private Button acceptButton;

    public TransferViewHolder(android.view.View itemView) {
      super(itemView);
      senderName = (TextView) itemView.findViewById(R.id.sender_info);
      appIcon = (ImageView) itemView.findViewById(R.id.transfer_app_icon);
      acceptButton = (Button) itemView.findViewById(R.id.transfer_record_accept_app_button);
    }

    public void setTransferItem(AndroidAppInfo transferItem) {
      senderName.setText(transferItem.getPackageName());
      //// TODO: 06-07-2017 filipe create transfer model to represent the transfer object
      //appIcon.setImageDrawable(transferItem.getAppIcon());
      //if(transferItem.isSent()){
      // acceptButton.setVisibility(View.GONE);
      //}else{
      acceptButton.setOnClickListener(accept -> acceptSubject.onNext(transferItem));
    }
  }
}