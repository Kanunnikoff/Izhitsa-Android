/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.kanunnikoff.izhitsa.billing;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.ProductDetailsResponseListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManager implements PurchasesUpdatedListener {
    // Default value of mBillingClientResponseCode until BillingManager was not yeat initialized
    public static final int BILLING_MANAGER_NOT_INITIALIZED  = -1;

    private static final String TAG = "BillingManager";

    /** A reference to BillingClient **/
    private BillingClient mBillingClient;

    /**
     * True if billing service is connected now.
     */
    private boolean mIsServiceConnected;

    private final BillingUpdatesListener mBillingUpdatesListener;

    private final Activity mActivity;

    private final List<Purchase> mPurchases = new ArrayList<>();

    private final Map<String, ProductDetails> mProductDetailsMap = new HashMap<>();

    private int mBillingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED;

    /* BASE_64_ENCODED_PUBLIC_KEY should be YOUR APPLICATION'S PUBLIC KEY
     * (that you got from the Google Play developer console). This is not your
     * developer public key, it's the *app-specific* public key.
     *
     * Instead of just storing the entire literal string here embedded in the
     * program,  construct the key at runtime from pieces or
     * use bit manipulation (for example, XOR with some other string) to hide
     * the actual key.  The key itself is not secret information, but we don't
     * want to make it easy for an attacker to replace the public key with one
     * of their own and then fake messages from the server.
     */
    private static final String BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmWpZnD1rvQA5S0mLMgd2sxLpItopksrQ59A+vrceun1PqHapBnJuW+2ZE/8u8/Q/qZIiB16Ck9ZhDXmpDn5LaSMw3gFV+9hmeIMepkNrnHRR09XiQPodfQslTvBzyEhGaG3ZHbbo1iz8Lw3RykmMmjdG5I5ST62eU7Y5v7ZYVtsNCTj1NBl704cVmZrdHeJfqpSZHX2V88Bw6+jUSUQmlSucLD1IRz7G0ZZuu/I6cYjXD6ppiiW2S/bffWmfNl5epevGw8sFP5MIVBsX1DSC0mix46bA7oKe8e00uOLCHiCMu3W2BCrzPChXHcchRmNGpxtmtPJMZJ4BCqEVswC6cwIDAQAB";

    /**
     * Listener to the updates that happen when purchases list was updated or consumption of the
     * item was finished
     */
    public interface BillingUpdatesListener {
        void onBillingClientSetupFinished();
        void onPurchasesUpdated(List<Purchase> purchases);
    }

    public BillingManager(Activity activity, final BillingUpdatesListener updatesListener) {
        mActivity = activity;
        mBillingUpdatesListener = updatesListener;
        mBillingClient = BillingClient.newBuilder(mActivity)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build()
                )
                .build();

        // Start setup. This is asynchronous and the specified listener will be called
        // once setup completes.
        // It also starts to report all the new purchases through onPurchasesUpdated() callback.
        startServiceConnection(new Runnable() {
            @Override
            public void run() {
                // Notifying the listener that billing client is ready
                mBillingUpdatesListener.onBillingClientSetupFinished();
                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                queryPurchases();
            }
        });
    }

    /**
     * Handle a callback that purchases were updated from the Billing library
     */
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }

            mBillingUpdatesListener.onPurchasesUpdated(mPurchases);
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping");
        } else {
            Log.w(TAG, "onPurchasesUpdated() got responseCode: " + responseCode + ", debugMessage: " + billingResult.getDebugMessage());
        }
    }

    /**
     * Start a purchase flow
     */
    public void initiatePurchaseFlow(final String productId, final @BillingClient.ProductType String productType) {
        Runnable purchaseFlowRequest = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Launching in-app purchase flow for product: " + productId);
                QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType)
                        .build();
                QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                        .setProductList(Collections.singletonList(product))
                        .build();

                mBillingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult productDetailsResult) {
                        List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();
                        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK || productDetailsList == null || productDetailsList.isEmpty()) {
                            Log.w(TAG, "Failed to query product details: " + billingResult.getDebugMessage());
                            return;
                        }

                        ProductDetails productDetails = productDetailsList.get(0);
                        mProductDetailsMap.put(productId, productDetails);

                        BillingFlowParams.ProductDetailsParams.Builder detailsParamsBuilder =
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails);

                        if (BillingClient.ProductType.SUBS.equals(productType)) {
                            List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
                            if (offers != null && !offers.isEmpty()) {
                                detailsParamsBuilder.setOfferToken(offers.get(0).getOfferToken());
                            } else {
                                Log.w(TAG, "No subscription offers available for product: " + productId);
                            }
                        }

                        BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(Collections.singletonList(detailsParamsBuilder.build()))
                                .build();
                        mBillingClient.launchBillingFlow(mActivity, purchaseParams);
                    }
                });
            }
        };

        executeServiceRequest(purchaseFlowRequest);
    }

    public Context getContext() {
        return mActivity;
    }

    /**
     * Clear the resources
     */
    public void destroy() {
        Log.d(TAG, "Destroying the manager.");

        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }

    public void queryProductDetailsAsync(final @BillingClient.ProductType String itemType, final List<String> productIds, final ProductDetailsResponseListener listener) {
        // Creating a runnable from the request to use it inside our connection retry policy below
        Runnable queryRequest = new Runnable() {
            @Override
            public void run() {
                List<QueryProductDetailsParams.Product> products = new ArrayList<>();
                for (String productId : productIds) {
                    products.add(QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(itemType)
                            .build());
                }
                QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                        .setProductList(products)
                        .build();

                mBillingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
                    @Override
                    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult productDetailsResult) {
                        List<ProductDetails> productDetailsList = productDetailsResult.getProductDetailsList();
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                            for (ProductDetails details : productDetailsList) {
                                mProductDetailsMap.put(details.getProductId(), details);
                            }
                        }
                        listener.onProductDetailsResponse(billingResult, productDetailsResult);
                    }
                });
            }
        };

        executeServiceRequest(queryRequest);
    }

    /**
     * Returns the value Billing client response code or BILLING_MANAGER_NOT_INITIALIZED if the
     * client connection response was not received yet.
     */
    public int getBillingClientResponseCode() {
        return mBillingClientResponseCode;
    }

    /**
     * Handles the purchase
     * <p>Note: Notice that for each purchase, we check if signature is valid on the client.
     * It's recommended to move this check into your backend.
     * </p>
     * @param purchase Purchase to be handled
     */
    private void handlePurchase(Purchase purchase) {
        if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
            Log.i(TAG, "Got a purchase: " + purchase + "; but signature is bad. Skipping...");
            return;
        }

        Log.d(TAG, "Got a verified purchase: " + purchase);

        mPurchases.add(purchase);
    }

    private void updatePurchasesList(List<Purchase> purchases) {
        mPurchases.clear();
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
        mBillingUpdatesListener.onPurchasesUpdated(mPurchases);
    }

    /**
     * Checks if subscriptions are supported for current client
     * <p>Note: This method does not automatically retry for RESULT_SERVICE_DISCONNECTED.
     * It is only used in unit tests and after queryPurchases execution, which already has
     * a retry-mechanism implemented.
     * </p>
     */
    public boolean areSubscriptionsSupported() {
        BillingResult result = mBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS);

        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "areSubscriptionsSupported() got an error response: " + result.getResponseCode());
        }

        return result.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    public void queryPurchases() {
        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                queryPurchasesAsync(BillingClient.ProductType.INAPP, new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, List<Purchase> purchases) {
                        List<Purchase> combinedPurchases = new ArrayList<>();
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                            combinedPurchases.addAll(purchases);
                        } else {
                            Log.w(TAG, "queryPurchases() got an error response code: " + billingResult.getResponseCode());
                        }

                        if (areSubscriptionsSupported()) {
                            queryPurchasesAsync(BillingClient.ProductType.SUBS, new PurchasesResponseListener() {
                                @Override
                                public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, List<Purchase> purchases) {
                                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                                        combinedPurchases.addAll(purchases);
                                    } else {
                                        Log.w(TAG, "queryPurchases() got an error response code for subs: " + billingResult.getResponseCode());
                                    }
                                    updatePurchasesList(combinedPurchases);
                                }
                            });
                        } else {
                            updatePurchasesList(combinedPurchases);
                        }
                    }
                });
            }
        };

        executeServiceRequest(queryToExecute);
    }

    private void queryPurchasesAsync(@BillingClient.ProductType String productType, PurchasesResponseListener listener) {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build();
        mBillingClient.queryPurchasesAsync(params, listener);
    }

    public void startServiceConnection(final Runnable executeOnSuccess) {
        mBillingClient.startConnection(new BillingClientStateListener() {

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                Log.d(TAG, "Setup finished. Response code: " + billingResult.getResponseCode());

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    mIsServiceConnected = true;

                    if (executeOnSuccess != null) {
                        executeOnSuccess.run();
                    }
                }

                mBillingClientResponseCode = billingResult.getResponseCode();
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
            }
        });
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            // If billing service was disconnected, we try to reconnect 1 time.
            // (feel free to introduce your retry policy here).
            startServiceConnection(runnable);
        }
    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * <p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * </p>
     */
    private boolean verifyValidSignature(String signedData, String signature) {
        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)
        if (BASE_64_ENCODED_PUBLIC_KEY.contains("CONSTRUCT_YOUR")) {
            throw new RuntimeException("Please update your app's public key at: " + "BASE_64_ENCODED_PUBLIC_KEY");
        }

        try {
            return Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature);
        } catch (IOException e) {
            Log.e(TAG, "Got an exception trying to validate a purchase: " + e);
            return false;
        }
    }
}
