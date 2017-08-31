package cm.aptoide.pt.v8engine.billing;

import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.model.v3.ErrorResponse;
import cm.aptoide.pt.dataprovider.model.v3.InAppBillingPurchasesResponse;
import cm.aptoide.pt.dataprovider.model.v3.InAppBillingSkuDetailsResponse;
import cm.aptoide.pt.dataprovider.model.v3.PaidApp;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v3.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v3.GetApkInfoRequest;
import cm.aptoide.pt.dataprovider.ws.v3.InAppBillingAvailableRequest;
import cm.aptoide.pt.dataprovider.ws.v3.InAppBillingConsumeRequest;
import cm.aptoide.pt.dataprovider.ws.v3.InAppBillingPurchasesRequest;
import cm.aptoide.pt.dataprovider.ws.v3.InAppBillingSkuDetailsRequest;
import cm.aptoide.pt.dataprovider.ws.v3.V3;
import cm.aptoide.pt.v8engine.PackageRepository;
import cm.aptoide.pt.v8engine.billing.exception.ProductNotFoundException;
import cm.aptoide.pt.v8engine.billing.exception.PurchaseNotFoundException;
import cm.aptoide.pt.v8engine.billing.product.InAppPurchase;
import cm.aptoide.pt.v8engine.billing.product.PaidAppPurchase;
import cm.aptoide.pt.v8engine.billing.product.ProductFactory;
import cm.aptoide.pt.v8engine.billing.product.SimplePurchase;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Created by jose_messejana on 25-07-2017.
 */
//V8Engine.java
public class BitcoinBillingService implements BillingService {
    public static final boolean REALTRANSACTION = false;
    private final BodyInterceptor<BaseBody> bodyInterceptorV3;
    private final OkHttpClient httpClient;
    private final Converter.Factory converterFactory;
    private final TokenInvalidator tokenInvalidator;
    private final SharedPreferences sharedPreferences;
    private final PurchaseMapper purchaseMapper;
    private final ProductFactory productFactory;
    private final PackageRepository packageRepository;
    private final PaymentMethodMapper paymentMethodMapper;
    private final Resources resources;
    private final BillingIdResolver idResolver;
    private final int apiVersion;

    public BitcoinBillingService(BodyInterceptor<BaseBody> bodyInterceptorV3, OkHttpClient httpClient,
                                 Converter.Factory converterFactory, TokenInvalidator tokenInvalidator,
                                 SharedPreferences sharedPreferences, PurchaseMapper purchaseMapper,
                                 ProductFactory productFactory, PackageRepository packageRepository,
                                 PaymentMethodMapper paymentMethodMapper, Resources resources, BillingIdResolver idResolver,
                                 int apiVersion) {
        this.bodyInterceptorV3 = bodyInterceptorV3;
        this.httpClient = httpClient;
        this.converterFactory = converterFactory;
        this.tokenInvalidator = tokenInvalidator;
        this.sharedPreferences = sharedPreferences;
        this.purchaseMapper = purchaseMapper;
        this.productFactory = productFactory;
        this.packageRepository = packageRepository;
        this.paymentMethodMapper = paymentMethodMapper;
        this.resources = resources;
        this.idResolver = idResolver;
        this.apiVersion = apiVersion;
    }


    @Override
    public Single<List<PaymentMethod>> getPaymentMethods(Product product) {
        //if (product instanceof PaidAppProduct) {
            List<PaymentMethod> paymentList = new ArrayList<PaymentMethod>();
            paymentList.add(new PaymentMethod(100, "Bitcoin", "Coinbase Payment"));
            return Single.just(paymentList);
        /*}
        if (product instanceof InAppProduct) { //free
            return getServerSKUs(((InAppProduct) product).getApiVersion(),
                    ((InAppProduct) product).getPackageName(),
                    Collections.singletonList(((InAppProduct) product).getSku()),
                    ((InAppProduct) product).getType(), false).map(response -> response.getPaymentServices())
                    .map(response -> paymentMethodMapper.map(response));
        }

        throw new IllegalArgumentException("Invalid product. Must be "
                + InAppProduct.class.getSimpleName()
                + " or "
                + PaidAppProduct.class.getSimpleName());*/
    }

    @Override  public Completable getBilling(String sellerId, String type) {
        return InAppBillingAvailableRequest.of(apiVersion, idResolver.resolvePackageName(sellerId),
                type, bodyInterceptorV3, httpClient, converterFactory, tokenInvalidator, sharedPreferences)
                .observe()
                .toSingle()
                .flatMapCompletable(response -> {
                    if (response != null && response.isOk()) {
                        if (response.getInAppBillingAvailable()
                                .isAvailable()) {
                            return Completable.complete();
                        } else {
                            return Completable.error(new IllegalArgumentException(V3.getErrorMessage(response)));
                        }
                    } else {
                        return Completable.error(new IllegalArgumentException(V3.getErrorMessage(response)));
                    }
                });
    }

    @Override
    public Completable deletePurchase(String sellerId, String purchaseToken) {
        return InAppBillingConsumeRequest.of(apiVersion, idResolver.resolvePackageName(sellerId),
                purchaseToken, bodyInterceptorV3, httpClient, converterFactory, tokenInvalidator,
                sharedPreferences)
                .observe()
                .first()
                .toSingle()
                .flatMapCompletable(response -> {
                    if (response != null && response.isOk()) {
                        return Completable.complete();
                    }
                    if (isDeletionItemNotFound(response.getErrors())) {
                        return Completable.error(new PurchaseNotFoundException(V3.getErrorMessage(response)));
                    }
                    return Completable.error(new IllegalArgumentException(V3.getErrorMessage(response)));
                });
    }

    @Override
    public Single<List<Purchase>> getPurchases(String sellerId) {
        return Single.just(Collections.emptyList());
    }

    @Override
    public Single<List<Product>> getProducts(String sellerId, List<String> productIds) {
        return getServerSKUs(apiVersion, idResolver.resolvePackageName(sellerId),
                idResolver.resolveSkus(productIds), false).flatMap(
                response -> mapToProducts(apiVersion, idResolver.resolvePackageName(sellerId), response));
    }

    @Override public Single<Purchase> getPurchase(Product product) {
        return Single.just(new PaidAppPurchase("done", SimplePurchase.Status.COMPLETED, product.getId()));
    }

    @Override public Single<Purchase> getPurchase(String sellerId, String purchaseToken) {
        return getPurchases(sellerId).flatMapObservable(purchases -> Observable.from(purchases))
                .filter(purchase -> purchaseToken.equals(((InAppPurchase) purchase).getToken()))
                .first()
                .toSingle();
    }

    @Override
    public Single<Product> getProduct(String sellerId, String productId) {
        if (idResolver.isInAppId(productId)) {
            return getInAppProduct(sellerId, productId);
        }
        if (idResolver.isPaidAppId(productId)) {
            return getPaidAppProduct(productId);
        }

        return Single.error(new IllegalArgumentException("Invalid product id " + productId));
    }


    private Single<InAppBillingSkuDetailsResponse> getServerSKUs(int apiVersion, String packageName,
                                                                 List<String> skuList, boolean bypassCache) {
        return InAppBillingSkuDetailsRequest.of(apiVersion, packageName, skuList, bodyInterceptorV3,
                httpClient, converterFactory, tokenInvalidator, sharedPreferences)
                .observe(bypassCache)
                .first()
                .toSingle()
                .flatMap(response -> {
                    if (response != null && response.isOk()) {
                        return Single.just(response);
                    } else {
                        return Single.error(new IllegalArgumentException(V3.getErrorMessage(response)));
                    }
                });
    }

    private Observable<InAppBillingPurchasesResponse> getServerInAppPurchase(int apiVersion,
                                                                             String packageName, boolean bypassCache) {
        return packageRepository.getPackageVersionCode(packageName)
                .flatMapObservable(
                        packageVersionCode -> InAppBillingPurchasesRequest.of(apiVersion, packageName,
                                bodyInterceptorV3, httpClient, converterFactory, tokenInvalidator,
                                sharedPreferences, packageVersionCode)
                                .observe(bypassCache)
                                .flatMap(response -> {
                                    if (response != null && response.isOk()) {
                                        return Observable.just(response);
                                    }
                                    return Observable.error(
                                            new IllegalArgumentException(V3.getErrorMessage(response)));
                                }));
    }

    private boolean isDeletionItemNotFound(List<ErrorResponse> errors) {
        for (ErrorResponse error : errors) {
            if (error.code.equals("PRODUCT-201")) {
                return true;
            }
        }
        return false;
    }

    private Single<PaidApp> getServerPaidApp(boolean bypassCache, long appId) {
        return GetApkInfoRequest.of(appId, bodyInterceptorV3, httpClient, converterFactory,
                tokenInvalidator, sharedPreferences, resources)
                .observe(bypassCache)
                .flatMap(response -> {
                    if (response != null && response.isOk() && response.isPaid()) {
                        return Observable.just(response);
                    } else {
                        return Observable.error(new IllegalArgumentException(V3.getErrorMessage(response)));
                    }
                })
                .first()
                .toSingle();
    }

    private Single<List<Product>> mapToProducts(int apiVersion, String packageName,
                                                InAppBillingSkuDetailsResponse response) {
        return Single.zip(packageRepository.getPackageVersionCode(packageName),
                packageRepository.getPackageLabel(packageName),
                (packageVersionCode, applicationName) -> productFactory.create(apiVersion, packageName,
                        response, packageVersionCode, applicationName));
    }

    private Single<Product> getInAppProduct(String sellerId, String productId) {
        return getServerSKUs(apiVersion, idResolver.resolvePackageName(sellerId),
                Collections.singletonList(idResolver.resolveSku(productId)), false).flatMap(
                response -> mapToProducts(apiVersion, idResolver.resolvePackageName(sellerId), response))
                .flatMap(products -> {
                    if (products.isEmpty()) {
                        return Single.error(new ProductNotFoundException(
                                "No product found for sku: " + idResolver.resolveSku(productId)));
                    }
                    return Single.just(products.get(0));
                });
    }

    public Single<Product> getPaidAppProduct(String productId) {
        return getServerPaidApp(false, idResolver.resolveAppId(productId)).map(
                paidApp -> productFactory.create(paidApp));
    }
}