/*
 * Copyright (c) 2016.
 * Modified by SithEngineer on 20/07/2016.
 */

package cm.aptoide.pt.dataprovider.ws.v7;

import cm.aptoide.pt.dataprovider.BuildConfig;
import cm.aptoide.pt.dataprovider.ws.BaseBodyDecorator;
import cm.aptoide.pt.model.v7.BaseV7Response;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import rx.Observable;

/**
 * Created by sithengineer on 20/07/16.
 */
public class PostCommentRequest extends V7<BaseV7Response, PostCommentRequest.Body> {

  private static final String BASE_HOST = BuildConfig.APTOIDE_WEB_SERVICES_SCHEME + "://" + BuildConfig.APTOIDE_WEB_SERVICES_WRITE_V7_HOST + "/api/7/";

  protected PostCommentRequest(Body body, String baseHost) {
    super(body, baseHost);
  }

  public static PostCommentRequest of(long reviewId, String text, String accessToken, String email,
      String aptoideClientUUID) {
    //
    //  http://ws75-primary.aptoide.com/api/7/setComment/review_id/1/body/amazing%20review/access_token/ca01ee1e05ab4d82d99ef143e2816e667333c6ef
    //
    BaseBodyDecorator decorator = new BaseBodyDecorator(aptoideClientUUID);
    Body body = new Body(reviewId, text);
    return new PostCommentRequest((Body) decorator.decorate(body, accessToken), BASE_HOST);
  }

  @Override protected Observable<BaseV7Response> loadDataFromNetwork(Interfaces interfaces,
      boolean bypassCache) {
    return interfaces.postComment(body, true);
  }

  @Data @Accessors(chain = false) @EqualsAndHashCode(callSuper = true) public static class Body
      extends BaseBody {

    private long reviewId;
    private String body;

    public Body(long reviewId, String text) {
      this.reviewId = reviewId;
      this.body = text;
    }
  }
}