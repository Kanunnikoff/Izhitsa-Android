package software.kanunnikoff.izhitsa.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import android.content.Context
import software.kanunnikoff.izhitsa.Core
import software.kanunnikoff.izhitsa.Core.PRICE
import software.kanunnikoff.izhitsa.Core.USD
import software.kanunnikoff.izhitsa.R
import software.kanunnikoff.izhitsa.billing.BillingManager
import software.kanunnikoff.izhitsa.billing.BillingProvider
import software.kanunnikoff.izhitsa.billing.MainViewController
import software.kanunnikoff.izhitsa.percentOf

class MainActivity : AppCompatActivity(), BillingProvider {
    var billingManager: BillingManager? = null
    private var viewController: MainViewController? = null
    private var firebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.menuButton).setOnClickListener {
            MenuBottomSheet().show(supportFragmentManager, MenuBottomSheet.TAG)
        }

        Core.sharedPreferences = getSharedPreferences(Core.APP_TAG, Context.MODE_PRIVATE)

// ------------------------------------------- In-App Billing

        viewController = MainViewController(this)
        billingManager = BillingManager(this, viewController!!.updateListener)

// ------------------------------------------- Firebase Analytics

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

    }

    override fun isPremiumPurchased(): Boolean {
        return viewController!!.isPremiumPurchased
    }

    fun onBillingManagerSetupFinished() {
        Log.d(TAG, "In-App Billing client is configured")
    }

    @UiThread
    fun premiumPurchased() {  // покупка подтверждена
        if (!Core.isPremiumPurchased) {
            Core.isPremiumPurchased = true

            findViewById<View>(android.R.id.content)?.let { rootView ->
                Snackbar.make(rootView, getString(R.string.premium_purchased), Snackbar.LENGTH_LONG).show()
            }

            val checkoutItem = Bundle().apply {
                putString(FirebaseAnalytics.Param.ITEM_ID, Core.PREMIUM_SKU_ID)
                putString(FirebaseAnalytics.Param.ITEM_NAME, "Premium")
                putLong(FirebaseAnalytics.Param.QUANTITY, 1L)
                putDouble(FirebaseAnalytics.Param.PRICE, PRICE.toDouble())
            }
            val checkoutParams = Bundle().apply {
                putString(FirebaseAnalytics.Param.CURRENCY, USD.currencyCode)
                putDouble(FirebaseAnalytics.Param.VALUE, PRICE.toDouble())
                putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(checkoutItem))
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, checkoutParams)

            val purchaseParams = Bundle().apply {
                putString(FirebaseAnalytics.Param.CURRENCY, USD.currencyCode)
                putDouble(FirebaseAnalytics.Param.VALUE, (70 percentOf PRICE).toDouble())
                putString(FirebaseAnalytics.Param.TRANSACTION_ID, Core.PREMIUM_SKU_ID)
            }
            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.PURCHASE, purchaseParams)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
