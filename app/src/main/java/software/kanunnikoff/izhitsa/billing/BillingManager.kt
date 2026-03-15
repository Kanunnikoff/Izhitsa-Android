package software.kanunnikoff.izhitsa.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import java.io.IOException

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
class BillingManager(private val mActivity: Activity, private val mBillingUpdatesListener: BillingUpdatesListener) : PurchasesUpdatedListener {

    private var mBillingClient: BillingClient? = null
    private var mIsServiceConnected: Boolean = false
    private val mPurchases = mutableListOf<Purchase>()
    private val mProductDetailsMap = mutableMapOf<String, ProductDetails>()
    var billingClientResponseCode: Int = BILLING_MANAGER_NOT_INITIALIZED
        private set

    companion object {
        const val BILLING_MANAGER_NOT_INITIALIZED = -1
        private const val TAG = "BillingManager"
        private const val BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmWpZnD1rvQA5S0mLMgd2sxLpItopksrQ59A+vrceun1PqHapBnJuW+2ZE/8u8/Q/qZIiB16Ck9ZhDXmpDn5LaSMw3gFV+9hmeIMepkNrnHRR09XiQPodfQslTvBzyEhGaG3ZHbbo1iz8Lw3RykmMmjdG5I5ST62eU7Y5v7ZYVtsNCTj1NBl704cVmZrdHeJfqpSZHX2V88Bw6+jUSUQmlSucLD1IRz7G0ZZuu/I6cYjXD6ppiiW2S/bffWmfNl5epevGw8sFP5MIVBsX1DSC0mix46bA7oKe8e00uOLCHiCMu3W2BCrzPChXHcchRmNGpxtmtPJMZJ4BCqEVswC6cwIDAQAB"
    }

    interface BillingUpdatesListener {
        fun onBillingClientSetupFinished()
        fun onPurchasesUpdated(purchases: List<Purchase>)
    }

    init {
        mBillingClient = BillingClient.newBuilder(mActivity)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        startServiceConnection {
            mBillingUpdatesListener.onBillingClientSetupFinished()
            Log.d(TAG, "Setup successful. Querying inventory.")
            queryPurchases()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        val responseCode = billingResult.responseCode
        if (responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            mPurchases.clear()
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
            mBillingUpdatesListener.onPurchasesUpdated(mPurchases)
        } else if (responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping")
        } else {
            Log.w(TAG, "onPurchasesUpdated() got responseCode: $responseCode, debugMessage: ${billingResult.debugMessage}")
        }
    }

    fun initiatePurchaseFlow(productId: String, productType: String) {
        val purchaseFlowRequest = Runnable {
            Log.d(TAG, "Launching in-app purchase flow for product: $productId")
            val product = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()

            mBillingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                val productDetailsList = productDetailsResult.productDetailsList
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList == null || productDetailsList.isEmpty()) {
                    Log.w(TAG, "Failed to query product details: ${billingResult.debugMessage}")
                    return@queryProductDetailsAsync
                }

                val productDetails = productDetailsList[0]
                mProductDetailsMap[productId] = productDetails

                val detailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)

                if (BillingClient.ProductType.SUBS == productType) {
                    val offers = productDetails.subscriptionOfferDetails
                    if (offers != null && offers.isNotEmpty()) {
                        detailsParamsBuilder.setOfferToken(offers[0].offerToken)
                    } else {
                        Log.w(TAG, "No subscription offers available for product: $productId")
                    }
                }

                val purchaseParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(detailsParamsBuilder.build()))
                    .build()
                mBillingClient?.launchBillingFlow(mActivity, purchaseParams)
            }
        }

        executeServiceRequest(purchaseFlowRequest)
    }

    val context: Context
        get() = mActivity

    fun destroy() {
        Log.d(TAG, "Destroying the manager.")
        if (mBillingClient?.isReady == true) {
            mBillingClient?.endConnection()
            mBillingClient = null
        }
    }

    fun queryProductDetailsAsync(itemType: String, productIds: List<String>, listener: ProductDetailsResponseListener) {
        val queryRequest = Runnable {
            val products = productIds.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(itemType)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build()

            mBillingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                val productDetailsList = productDetailsResult.productDetailsList
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                    for (details in productDetailsList) {
                        mProductDetailsMap[details.productId] = details
                    }
                }
                listener.onProductDetailsResponse(billingResult, productDetailsResult)
            }
        }

        executeServiceRequest(queryRequest)
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
            Log.i(TAG, "Got a purchase: $purchase; but signature is bad. Skipping...")
            return
        }

        Log.d(TAG, "Got a verified purchase: $purchase")
        mPurchases.add(purchase)
    }

    private fun updatePurchasesList(purchases: List<Purchase>?) {
        mPurchases.clear()
        purchases?.forEach { handlePurchase(it) }
        mBillingUpdatesListener.onPurchasesUpdated(mPurchases)
    }

    fun areSubscriptionsSupported(): Boolean {
        val result = mBillingClient?.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "areSubscriptionsSupported() got an error response: ${result?.responseCode}")
        }
        return result?.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun queryPurchases() {
        val queryToExecute = Runnable {
            queryPurchasesAsync(BillingClient.ProductType.INAPP) { billingResult, purchases ->
                val combinedPurchases = mutableListOf<Purchase>()
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    combinedPurchases.addAll(purchases)
                } else {
                    Log.w(TAG, "queryPurchases() got an error response code: ${billingResult.responseCode}")
                }

                if (areSubscriptionsSupported()) {
                    queryPurchasesAsync(BillingClient.ProductType.SUBS) { subResult, subPurchases ->
                        if (subResult.responseCode == BillingClient.BillingResponseCode.OK && subPurchases != null) {
                            combinedPurchases.addAll(subPurchases)
                        } else {
                            Log.w(TAG, "queryPurchases() got an error response code for subs: ${subResult.responseCode}")
                        }
                        updatePurchasesList(combinedPurchases)
                    }
                } else {
                    updatePurchasesList(combinedPurchases)
                }
            }
        }
        executeServiceRequest(queryToExecute)
    }

    private fun queryPurchasesAsync(productType: String, listener: PurchasesResponseListener) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(productType)
            .build()
        mBillingClient?.queryPurchasesAsync(params, listener)
    }

    fun startServiceConnection(executeOnSuccess: Runnable?) {
        mBillingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "Setup finished. Response code: ${billingResult.responseCode}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    mIsServiceConnected = true
                    executeOnSuccess?.run()
                }
                billingClientResponseCode = billingResult.responseCode
            }

            override fun onBillingServiceDisconnected() {
                mIsServiceConnected = false
            }
        })
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (mIsServiceConnected) {
            runnable.run()
        } else {
            startServiceConnection(runnable)
        }
    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature)
        } catch (e: IOException) {
            Log.e(TAG, "Got an exception trying to validate a purchase: $e")
            false
        }
    }
}
