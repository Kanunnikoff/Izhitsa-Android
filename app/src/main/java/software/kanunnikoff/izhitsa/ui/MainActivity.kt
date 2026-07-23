package software.kanunnikoff.izhitsa.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.pm.PackageInfoCompat
import com.android.billingclient.api.BillingClient
import com.google.firebase.analytics.FirebaseAnalytics
import software.kanunnikoff.izhitsa.AppPreferences
import software.kanunnikoff.izhitsa.Core
import software.kanunnikoff.izhitsa.Core.PRICE
import software.kanunnikoff.izhitsa.Core.USD
import software.kanunnikoff.izhitsa.billing.BillingManager
import software.kanunnikoff.izhitsa.billing.BillingProvider
import software.kanunnikoff.izhitsa.billing.MainViewController
import software.kanunnikoff.izhitsa.percentOf

class MainActivity : AppCompatActivity(), BillingProvider {
    internal var billingManager: BillingManager? = null
    private var viewController: MainViewController? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var preferences: AppPreferences
    private var keyboardUsageState: MutableState<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Core.sharedPreferences = getSharedPreferences(
            Core.APP_TAG,
            Context.MODE_PRIVATE
        )
        preferences = AppPreferences(context = applicationContext)
        viewController = MainViewController(this)
        billingManager = BillingManager(this, viewController!!.updateListener)
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
                initialUsesSystemFont = preferences.usesSystemFont,
                initialUsesPreRevolutionaryOrthography =
                    preferences.usesPreRevolutionaryOrthography,
                hasUsedKeyboard = hasUsedKeyboard.value,
                versionName = versionName,
                versionCode = versionCode,
                onUsesSystemFontChanged = { value ->
                    preferences.usesSystemFont = value
                },
                onUsesPreRevolutionaryOrthographyChanged = { value ->
                    preferences.usesPreRevolutionaryOrthography = value
                },
                onSupportAuthor = ::supportAuthor
            )
        }

        ReviewPrompter(
            activity = this,
            preferences = preferences
        ).requestIfNeeded(currentVersion = versionCode.toString())
    }

    override fun onResume() {
        super.onResume()

        if (::preferences.isInitialized) {
            keyboardUsageState?.value = preferences.hasUsedKeyboard
        }
    }

    override fun onDestroy() {
        billingManager?.destroy()
        billingManager = null

        super.onDestroy()
    }

    override fun isPremiumPurchased(): Boolean {
        return viewController?.isPremiumPurchased == true
    }

    fun onBillingManagerSetupFinished() {
        Log.d(Tag, "Клиент платежей настроен.")
    }

    @UiThread
    fun premiumPurchased() {
        if (Core.isPremiumPurchased) {
            return
        }

        Core.isPremiumPurchased = true
        Toast.makeText(
            this,
            "Премного благодарю за поддержку!",
            Toast.LENGTH_LONG
        ).show()

        val checkoutItem = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, Core.PREMIUM_SKU_ID)
            putString(FirebaseAnalytics.Param.ITEM_NAME, "Поддержка автора")
            putLong(FirebaseAnalytics.Param.QUANTITY, 1L)
            putDouble(FirebaseAnalytics.Param.PRICE, PRICE.toDouble())
        }
        val checkoutParams = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, USD.currencyCode)
            putDouble(FirebaseAnalytics.Param.VALUE, PRICE.toDouble())
            putParcelableArray(
                FirebaseAnalytics.Param.ITEMS,
                arrayOf(checkoutItem)
            )
        }
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.BEGIN_CHECKOUT,
            checkoutParams
        )

        val purchaseParams = Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, USD.currencyCode)
            putDouble(
                FirebaseAnalytics.Param.VALUE,
                (GooglePlayRevenueShare percentOf PRICE).toDouble()
            )
            putString(
                FirebaseAnalytics.Param.TRANSACTION_ID,
                Core.PREMIUM_SKU_ID
            )
        }
        firebaseAnalytics.logEvent(
            FirebaseAnalytics.Event.PURCHASE,
            purchaseParams
        )
    }

    private fun supportAuthor() {
        if (Core.isPremiumPurchased || isPremiumPurchased()) {
            Toast.makeText(
                this,
                "Вы уже поддержали автора. Сердечное спасибо!",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        billingManager?.initiatePurchaseFlow(
            productId = Core.PREMIUM_SKU_ID,
            productType = BillingClient.ProductType.INAPP
        )
    }

    companion object {
        private const val Tag = "MainActivity"
        private const val GooglePlayRevenueShare = 70
    }
}
