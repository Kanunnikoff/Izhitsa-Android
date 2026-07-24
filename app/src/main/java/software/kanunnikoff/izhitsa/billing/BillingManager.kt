package software.kanunnikoff.izhitsa.billing

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import java.io.IOException

/**
 * Управляет покупкой расходуемых чаевых через Google Play.
 *
 * @property activity окно, из которого открывается системная оплата.
 * @property billingUpdatesListener получатель состояния платёжного процесса.
 */
class BillingManager(
    private val activity: Activity,
    private val billingUpdatesListener: BillingUpdatesListener
) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enableAutoServiceReconnection()
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()
    private val purchaseTokensBeingConsumed = mutableSetOf<String>()
    private var tipsProductDetails: ProductDetails? = null
    private var isBillingAvailable = false
    private var isPurchaseLaunchInProgress = false

    init {
        startServiceConnection()
    }

    /** Загружает сведения о чаевых и открывает системное окно разовой покупки. */
    fun initiateTipsPurchase() {
        if (!isBillingAvailable || !billingClient.isReady) {
            Log.w(TAG, "Платёжная служба недоступна.")
            billingUpdatesListener.onBillingError()
            return
        }

        if (isPurchaseLaunchInProgress) {
            Log.i(TAG, "Открытие системного окна оплаты уже выполняется.")
            return
        }

        isPurchaseLaunchInProgress = true

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(TIPS_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(queryParams) { billingResult, productDetailsResult ->
            val productDetails = productDetailsResult.productDetailsList
                .singleOrNull { details ->
                    details.productId == TIPS_PRODUCT_ID
                }

            if (
                billingResult.responseCode != BillingClient.BillingResponseCode.OK ||
                productDetails == null
            ) {
                Log.e(
                    TAG,
                    "Не удалось получить сведения о чаевых: ${billingResult.debugMessage}"
                )
                isPurchaseLaunchInProgress = false
                billingUpdatesListener.onBillingError()
                return@queryProductDetailsAsync
            }

            tipsProductDetails = productDetails

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
            val launchResult = billingClient.launchBillingFlow(
                activity,
                billingFlowParams
            )

            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(
                    TAG,
                    "Не удалось открыть системное окно оплаты: ${launchResult.debugMessage}"
                )
                isPurchaseLaunchInProgress = false
                billingUpdatesListener.onBillingError()
            }
        }
    }

    /**
     * Принимает итог системного окна оплаты и направляет покупки на проверку.
     *
     * @param billingResult результат операции Google Play.
     * @param purchases возвращённые покупки либо `null`.
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val tipsPurchases = purchases
                    .orEmpty()
                    .filter { purchase ->
                        purchase.products.contains(TIPS_PRODUCT_ID)
                    }

                if (tipsPurchases.isEmpty()) {
                    Log.e(TAG, "Google Play не вернул покупку чаевых.")
                    isPurchaseLaunchInProgress = false
                    billingUpdatesListener.onBillingError()
                } else {
                    tipsPurchases.forEach { purchase ->
                        handleTipsPurchase(purchase = purchase)
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "Пользователь отменил покупку чаевых.")
                isPurchaseLaunchInProgress = false
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                /*
                 * Расходуемый товар может остаться неосвобождённым, если приложение было
                 * закрыто сразу после оплаты. Запрашиваем покупку снова и завершаем её.
                 */
                queryTipsPurchases(notifyAboutError = true)
            }

            else -> {
                Log.e(
                    TAG,
                    "Ошибка покупки ${billingResult.responseCode}: ${billingResult.debugMessage}"
                )
                isPurchaseLaunchInProgress = false
                billingUpdatesListener.onBillingError()
            }
        }
    }

    /**
     * Проверяет подпись и состояние покупки перед её погашением.
     *
     * @param purchase проверяемая покупка.
     */
    private fun handleTipsPurchase(purchase: Purchase) {
        if (!verifyValidSignature(purchase.originalJson, purchase.signature)) {
            Log.e(TAG, "Подпись покупки чаевых недействительна.")
            isPurchaseLaunchInProgress = false
            billingUpdatesListener.onBillingError()
            return
        }

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> {
                Log.i(TAG, "Покупка чаевых ожидает завершения.")
                isPurchaseLaunchInProgress = false
            }

            Purchase.PurchaseState.PURCHASED -> {
                consumeTipsPurchase(purchase = purchase)
            }

            else -> {
                Log.e(TAG, "Покупка чаевых находится в неизвестном состоянии.")
                isPurchaseLaunchInProgress = false
                billingUpdatesListener.onBillingError()
            }
        }
    }

    /**
     * Погашает расходуемую покупку, чтобы чаевые можно было приобрести повторно.
     *
     * Набор [purchaseTokensBeingConsumed] защищает один токен от параллельных запросов.
     *
     * @param purchase подтверждённая покупка.
     */
    private fun consumeTipsPurchase(purchase: Purchase) {
        val purchaseToken = purchase.purchaseToken
        if (!purchaseTokensBeingConsumed.add(purchaseToken)) {
            return
        }

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, _ ->
            purchaseTokensBeingConsumed.remove(purchaseToken)
            isPurchaseLaunchInProgress = false

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                billingUpdatesListener.onTipsPurchased(
                    purchase = purchase,
                    productDetails = tipsProductDetails
                )
            } else {
                Log.e(
                    TAG,
                    "Не удалось завершить покупку чаевых: ${billingResult.debugMessage}"
                )
                billingUpdatesListener.onBillingError()
            }
        }
    }

    /**
     * Находит ранее оплаченные, но ещё не погашенные чаевые.
     *
     * @param notifyAboutError сообщать ли слушателю об ошибке или пустом результате.
     */
    private fun queryTipsPurchases(notifyAboutError: Boolean) {
        val queryParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(queryParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val tipsPurchases = purchases
                    .filter { purchase ->
                        purchase.products.contains(TIPS_PRODUCT_ID)
                    }

                tipsPurchases.forEach { purchase ->
                    handleTipsPurchase(purchase = purchase)
                }

                if (notifyAboutError && tipsPurchases.isEmpty()) {
                    isPurchaseLaunchInProgress = false
                    billingUpdatesListener.onBillingError()
                }
            } else {
                Log.e(
                    TAG,
                    "Не удалось проверить незавершённые чаевые: ${billingResult.debugMessage}"
                )

                if (notifyAboutError) {
                    isPurchaseLaunchInProgress = false
                    billingUpdatesListener.onBillingError()
                }
            }
        }
    }

    /** Устанавливает соединение с Google Play и восстанавливает незавершённые покупки. */
    private fun startServiceConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            /**
             * Сохраняет доступность платежей и запускает восстановление покупок.
             *
             * @param billingResult результат настройки клиента.
             */
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isBillingAvailable =
                    billingResult.responseCode == BillingClient.BillingResponseCode.OK
                Log.d(
                    TAG,
                    "Платёжный клиент настроен с кодом ${billingResult.responseCode}: " +
                        billingResult.debugMessage
                )

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    billingUpdatesListener.onBillingClientSetupFinished()
                    queryTipsPurchases(notifyAboutError = false)
                }
            }

            /** Отмечает разрыв; автоматическое переподключение выполнит библиотека. */
            override fun onBillingServiceDisconnected() {
                isBillingAvailable = false
                Log.w(
                    TAG,
                    "Соединение с платёжной службой потеряно; библиотека восстановит его."
                )
            }
        })
    }

    /** Освобождает соединение с Google Play; вызывается при уничтожении окна. */
    fun destroy() {
        purchaseTokensBeingConsumed.clear()
        billingClient.endConnection()
    }

    /**
     * Проверяет криптографическую подпись ответа Google Play открытым ключом приложения.
     *
     * @param signedData исходные подписанные данные покупки.
     * @param signature подпись в Base64.
     */
    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return try {
            Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature)
        } catch (exception: IOException) {
            Log.e(
                TAG,
                "Не удалось проверить подпись покупки.",
                exception
            )
            false
        }
    }

    /** Получает изменения состояния платёжного процесса для отображения в интерфейсе. */
    interface BillingUpdatesListener {
        /** Вызывается после успешной подготовки платёжного клиента. */
        fun onBillingClientSetupFinished()

        /**
         * Сообщает об успешно проверенных и погашенных чаевых.
         *
         * @param purchase подтверждённая покупка.
         * @param productDetails сведения о цене либо `null`, если они уже недоступны.
         */
        fun onTipsPurchased(
            purchase: Purchase,
            productDetails: ProductDetails?
        )

        /** Сообщает об ошибке, которую нужно показать пользователю. */
        fun onBillingError()
    }

    companion object {
        /** Идентификатор расходуемых чаевых в Google Play. */
        const val TIPS_PRODUCT_ID = "tips"

        private const val TAG = "BillingManager"
        private const val BASE_64_ENCODED_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmWpZnD1rvQA5S0mLMgd2sxLpItopksrQ59A+vrceun1PqHapBnJuW+2ZE/8u8/Q/qZIiB16Ck9ZhDXmpDn5LaSMw3gFV+9hmeIMepkNrnHRR09XiQPodfQslTvBzyEhGaG3ZHbbo1iz8Lw3RykmMmjdG5I5ST62eU7Y5v7ZYVtsNCTj1NBl704cVmZrdHeJfqpSZHX2V88Bw6+jUSUQmlSucLD1IRz7G0ZZuu/I6cYjXD6ppiiW2S/bffWmfNl5epevGw8sFP5MIVBsX1DSC0mix46bA7oKe8e00uOLCHiCMu3W2BCrzPChXHcchRmNGpxtmtPJMZJ4BCqEVswC6cwIDAQAB"
    }
}
