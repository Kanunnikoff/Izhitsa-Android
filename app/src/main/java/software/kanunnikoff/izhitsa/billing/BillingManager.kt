package software.kanunnikoff.izhitsa.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import java.io.IOException
import java.util.ArrayList

class BillingManager(
    private val activity: Activity,
    private val billingUpdatesListener: BillingUpdatesListener
) : PurchasesUpdatedListener {

    companion object {
        const val BILLING_MANAGER_NOT_INITIALIZED = -1
        private const val TAG = "BillingManager"
        private const val BASE_64_ENCODED_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmWpZnD1rvQA5S0mLMgd2sxLpItopksrQ59A+vrceun1PqHapBnJuW+2ZE/8u8/Q/qZIiB16Ck9ZhDXmpDn5LaSMw3gFV+9hmeIMepkNrnHRR09XiQPodfQslTvBzyEhGaG3ZHbbo1iz8Lw3RykmMmjdG5I5ST62eU7Y5v7ZYVtsNCTj1NBl704cVmZrdHeJfqpSZHX2V88Bw6+jUSUQmlSucLD1IRz7G0ZZuu/I6cYjXD6ppiiW2S/bffWmfNl5epevGw8sFP5MIVBsX1DSC0mix46bA7oKe8e00uOLCHiCMu3W2BCrzPChXHcchRmNGpxtmtPJMZJ4BCqEVswC6cwIDAQAB"
    }

    interface BillingUpdatesListener {
        fun onBillingClientSetupFinished()
        fun onPurchasesUpdated(purchases: List<Purchase>)
    }

    private var billingClient: BillingClient? = null
    private var isServiceConnected = false
    private val purchases: MutableList<Purchase> = ArrayList()
    private var _billingClientResponseCode = BILLING_MANAGER_NOT_INITIALIZED
    val billingClientResponseCode: Int
        get() = _billingClientResponseCode

    init {
        val pendingParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()
        billingClient = BillingClient.newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases(pendingParams)
            .build()

        startServiceConnection {
            billingUpdatesListener.onBillingClientSetupFinished()
            Log.d(TAG, "Setup successful. Querying inventory.")
            queryPurchases()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
            billingUpdatesListener.onPurchasesUpdated(this.purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "onPurchasesUpdated() - user cancelled the purchase flow - skipping")
        } else {
            Log.w(TAG, "onPurchasesUpdated() got unknown resultCode: ${billingResult.responseCode}")
        }
    }

    fun initiatePurchaseFlow(productId: String, billingType: String) {
        val productList = ArrayList<QueryProductDetailsParams.Product>()
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(billingType)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, queryResult ->
            val productDetailsList: List<ProductDetails> = queryResult.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetailsParamsList = ArrayList<BillingFlowParams.ProductDetailsParams>()
                productDetailsParamsList.add(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetailsList[0])
                        .build()
                )

                val purchaseParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                billingClient?.launchBillingFlow(activity, purchaseParams)
            }
        }
    }

    fun destroy() {
        Log.d(TAG, "Destroying the manager.")
        if (billingClient != null && billingClient!!.isReady) {
            billingClient!!.endConnection()
            billingClient = null
        }
    }


    private fun handlePurchase(purchase: Purchase) {
        if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
            Log.i(TAG, "Got a purchase: $purchase; but signature is bad. Skipping...")
            return
        }
        Log.d(TAG, "Got a verified purchase: $purchase")
        purchases.add(purchase)
    }

    fun queryPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                this.purchases.clear()
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
                billingUpdatesListener.onPurchasesUpdated(this.purchases)
            }
        }
    }

    fun startServiceConnection(executeOnSuccess: Runnable?) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "Setup finished. Response code: ${billingResult.responseCode}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isServiceConnected = true
                    executeOnSuccess?.run()
                }
                _billingClientResponseCode = billingResult.responseCode
            }

            override fun onBillingServiceDisconnected() {
                isServiceConnected = false
            }
        })
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
