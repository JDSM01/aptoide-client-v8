package cm.aptoide.pt.v8engine.social.view;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import cm.aptoide.pt.v8engine.social.data.Card;
import cm.aptoide.pt.v8engine.social.data.CardViewHolderFactory;
import java.util.List;

/**
 * Created by jdandrade on 2/06/17.
 */

public class CardAdapter extends RecyclerView.Adapter<CardViewHolder> {

  private final CardViewHolderFactory cardViewHolderFactory;
  private List<Card> cards;
  private ProgressCard progressCard;

  public CardAdapter(List<Card> cards, CardViewHolderFactory cardViewHolderFactory,
      ProgressCard progressCard) {
    this.cards = cards;
    this.cardViewHolderFactory = cardViewHolderFactory;
    this.progressCard = progressCard;
  }

  @Override public CardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return cardViewHolderFactory.createViewHolder(viewType, parent);
  }

  @Override public void onBindViewHolder(CardViewHolder holder, int position) {
    holder.setCard(cards.get(position));
  }

  @Override public int getItemViewType(int position) {
    return cards.get(position)
        .getType()
        .ordinal();
  }

  @Override public int getItemCount() {
    return this.cards.size();
  }

  public void updateCards(List<Card> cards) {
    this.cards = cards;
    notifyDataSetChanged();
  }

  public void addCards(List<Card> cards) {
    this.cards.addAll(cards);
    notifyDataSetChanged();
  }

  public void addLoadMoreProgress() {
    this.cards.add(progressCard);
    notifyDataSetChanged();
  }

  public void removeLoadMoreProgress() {
    this.cards.remove(progressCard);
  }
}
