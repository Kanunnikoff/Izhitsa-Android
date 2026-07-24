package software.kanunnikoff.izhitsa.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.pm.PackageInfoCompat
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.google.firebase.analytics.FirebaseAnalytics
import software.kanunnikoff.izhitsa.AppPreferences
import software.kanunnikoff.izhitsa.R
import software.kanunnikoff.izhitsa.billing.BillingManager

/**
 * Главное окно приложения: показывает справку, алфавит, настройки и сведения
 * о программе, а также связывает интерфейс с оплатой чаевых и аналитикой.
 */
class MainActivity : AppCompatActivity(), BillingManager.BillingUpdatesListener {
    internal var billingManager: BillingManager? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var preferences: AppPreferences
    private var keyboardUsageState: MutableState<Boolean>? = null

    /**
     * Создаёт зависимости окна и корневой интерфейс Compose.
     *
     * @param savedInstanceState сохранённое состояние предыдущего экземпляра окна.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = AppPreferences(context = applicationContext)
        billingManager = BillingManager(
            activity = this,
            billingUpdatesListener = this
        )
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val versionName = packageManager
            .getPackageInfo(packageName, 0)
            .versionName
            .orEmpty()
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        keyboardUsageState = mutableStateOf(preferences.hasUsedKeyboard)

        setContent {
            val hasUsedKeyboard = checkNotNull(keyboardUsageState)

            IzhitsaApplication(
                initialUsesPreRevolutionaryOrthography =
                    preferences.usesPreRevolutionaryOrthography,
                initialIsKeyboardSoundFeedbackEnabled =
                    preferences.isKeyboardSoundFeedbackEnabled,
                initialIsKeyboardHapticFeedbackEnabled =
                    preferences.isKeyboardHapticFeedbackEnabled,
                hasUsedKeyboard = hasUsedKeyboard.value,
                versionName = versionName,
                versionCode = versionCode,
                onUsesPreRevolutionaryOrthographyChanged = { value ->
                    preferences.usesPreRevolutionaryOrthography = value
                },
                onKeyboardSoundFeedbackChanged = { value ->
                    preferences.isKeyboardSoundFeedbackEnabled = value
                },
                onKeyboardHapticFeedbackChanged = { value ->
                    preferences.isKeyboardHapticFeedbackEnabled = value
                },
                onSupportAuthor = ::supportAuthor
            )
        }

        ReviewPrompter(
            activity = this,
            preferences = preferences
        ).requestIfNeeded(currentVersion = versionCode.toString())
    }

    /** Обновляет отметку использования клавиатуры после возврата из другого приложения. */
    override fun onResume() {
        super.onResume()

        if (::preferences.isInitialized) {
            keyboardUsageState?.value = preferences.hasUsedKeyboard
        }
    }

    /** Закрывает платёжное соединение вместе с окном. */
    override fun onDestroy() {
        billingManager?.destroy()
        billingManager = null

        super.onDestroy()
    }

    /** Записывает успешную подготовку платёжного клиента в журнал. */
    override fun onBillingClientSetupFinished() {
        Log.d(Tag, "Клиент платежей настроен.")
    }

    /**
     * Благодарит пользователя и регистрирует подтверждённую покупку в аналитике.
     *
     * @param purchase подтверждённая покупка.
     * @param productDetails сведения о цене либо `null`.
     */
    override fun onTipsPurchased(
        purchase: Purchase,
        productDetails: ProductDetails?
    ) {
        runOnUiThread {
            Toast.makeText(
                this,
                R.string.tips_acquired,
                Toast.LENGTH_LONG
            ).show()
        }

        // Цена Google Play задана в миллионных долях валютной единицы.
        val offerDetails = productDetails?.oneTimePurchaseOfferDetails
        val price = offerDetails
            ?.priceAmountMicros
            ?.toDouble()
            ?.div(MicrosPerCurrencyUnit)
        val quantity = purchase.quantity.toLong()
        val purchaseItem = Bundle().apply {
            putString(
                FirebaseAnalytics.Param.ITEM_ID,
                BillingManager.TIPS_PRODUCT_ID
            )
            putString(FirebaseAnalytics.Param.ITEM_NAME, TipsItemName)
            putLong(FirebaseAnalytics.Param.QUANTITY, quantity)

            price?.let { value ->
                putDouble(FirebaseAnalytics.Param.PRICE, value)
            }
        }
        val purchaseParams = Bundle().apply {
            offerDetails?.priceCurrencyCode?.let { currencyCode ->
                putString(FirebaseAnalytics.Param.CURRENCY, currencyCode)
            }
            price?.let { value ->
                putDouble(
                    FirebaseAnalytics.Param.VALUE,
                    value * quantity
                )
            }
            purchase.orderId?.let { orderId ->
                putString(FirebaseAnalytics.Param.TRANSACTION_ID, orderId)
            }
            putParcelableArray(
                FirebaseAnalytics.Param.ITEMS,
                arrayOf(purchaseItem)
            )
        }

        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.PURCHASE,
            purchaseParams
        )
    }

    /** Показывает понятное сообщение при любой ошибке платёжного процесса. */
    override fun onBillingError() {
        Log.e(Tag, "Не удалось открыть оплату чаевых.")

        runOnUiThread {
            Toast.makeText(
                this,
                R.string.couldnt_make_purchase,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Регистрирует намерение поддержать автора и запускает покупку чаевых. */
    internal fun supportAuthor() {
        val addToCartParams = Bundle().apply {
            putString(
                FirebaseAnalytics.Param.ITEM_ID,
                BillingManager.TIPS_PRODUCT_ID
            )
            putString(FirebaseAnalytics.Param.ITEM_NAME, TipsItemName)
            putString(
                FirebaseAnalytics.Param.ITEM_CATEGORY,
                InAppPurchaseCategory
            )
        }
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.ADD_TO_CART,
            addToCartParams
        )

        billingManager?.initiateTipsPurchase() ?: onBillingError()
    }

    companion object {
        private const val Tag = "MainActivity"
        private const val TipsItemName = "Чаевые"
        private const val InAppPurchaseCategory = "Покупки в приложении"
        private const val MicrosPerCurrencyUnit = 1_000_000.0
    }
}
