package com.alaruss.verbs.premium;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {
    public static final String PRODUCT_ID_PREMIUM = "premium_unlock";
    private static final String TAG = "BillingManager";
    private final Context context;
    private final PremiumManager premiumManager;
    private BillingClient billingClient;
    private ProductDetails premiumProductDetails;
    private PurchaseCallback purchaseCallback;

    public BillingManager(Context context) {
        this.context = context.getApplicationContext();
        this.premiumManager = PremiumManager.getInstance(context);
        setupBillingClient();
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build())
                .build();
    }

    public void startConnection(Runnable onConnected) {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected");
                    queryProductDetails();
                    queryExistingPurchases();
                    if (onConnected != null) {
                        onConnected.run();
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected");
            }
        });
    }

    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult,
                                                 @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<ProductDetails> productDetailsList = queryProductDetailsResult.getProductDetailsList();
                    for (ProductDetails details : productDetailsList) {
                        if (PRODUCT_ID_PREMIUM.equals(details.getProductId())) {
                            premiumProductDetails = details;
                            Log.d(TAG, "Premium product details loaded");
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to query product details: " + billingResult.getDebugMessage());
                }
            }
        });
    }

    private void queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult,
                                                         @NonNull List<Purchase> purchases) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : purchases) {
                                if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                    handlePurchase(purchase);
                                }
                            }
                        }
                    }
                }
        );
    }

    public void launchPurchaseFlow(Activity activity, PurchaseCallback callback) {
        this.purchaseCallback = callback;

        if (premiumProductDetails == null) {
            Log.w(TAG, "Product details not loaded, attempting to reconnect...");
            // Try to reconnect and load product details
            if (!billingClient.isReady()) {
                startConnection(() -> {
                    // Retry after connection
                    if (premiumProductDetails != null) {
                        launchPurchaseFlowInternal(activity, callback);
                    } else {
                        Log.e(TAG, "Product details still null after reconnection");
                        if (callback != null) {
                            callback.onPurchaseFailed("Product not available. Please try again later.");
                        }
                    }
                });
            } else {
                // Client is ready but no product details - query again
                queryProductDetails();
                if (callback != null) {
                    callback.onPurchaseFailed("Loading product details. Please try again.");
                }
            }
            return;
        }

        launchPurchaseFlowInternal(activity, callback);
    }

    private void launchPurchaseFlowInternal(Activity activity, PurchaseCallback callback) {
        this.purchaseCallback = callback;

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(premiumProductDetails)
                .build());

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            if (callback != null) {
                callback.onPurchaseFailed("Failed to launch purchase flow: " + result.getDebugMessage());
            }
        }
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase cancelled by user");
            if (purchaseCallback != null) {
                purchaseCallback.onPurchaseCancelled();
            }
        } else {
            Log.e(TAG, "Purchase failed: " + billingResult.getDebugMessage());
            if (purchaseCallback != null) {
                purchaseCallback.onPurchaseFailed(billingResult.getDebugMessage());
            }
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Log.d(TAG, "Purchase verified by Google Play. Order ID: " + purchase.getOrderId());
            Log.d(TAG, "Purchase signature present: " + (purchase.getSignature() != null && !purchase.getSignature().isEmpty()));

            if (purchase.getProducts().contains(PRODUCT_ID_PREMIUM)) {
                premiumManager.setPremium(true);
                Log.d(TAG, "Premium unlocked!");

                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                }

                if (purchaseCallback != null) {
                    purchaseCallback.onPurchaseSuccess();
                }
            } else {
                Log.w(TAG, "Purchase does not contain expected product ID: " + PRODUCT_ID_PREMIUM);
                Log.w(TAG, "Products in purchase: " + purchase.getProducts());
            }
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged");
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
            }
        });
    }

    public String getFormattedPrice() {
        if (premiumProductDetails != null && premiumProductDetails.getOneTimePurchaseOfferDetails() != null) {
            return premiumProductDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
        }
        // Return null when price is unavailable - let UI handle the fallback
        return null;
    }

    public boolean isReady() {
        return billingClient.isReady() && premiumProductDetails != null;
    }

    public void endConnection() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }

    public interface PurchaseCallback {
        void onPurchaseSuccess();

        void onPurchaseFailed(String error);

        void onPurchaseCancelled();
    }
}
