package software.kanunnikoff.izhitsa.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.UiThread
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

// ------------------------------------------- Crashlytics
        // Firebase Crashlytics is initialized automatically via the gradle plugin
    }

    override fun isPremiumPurchased(): Boolean {
        return viewController!!.isPremiumPurchased()
    }

    fun onBillingManagerSetupFinished() {
        Log.d("MainActivity", "In-App Billing client is configured")
    }

    @UiThread
    fun premiumPurchased() {  // покупка подтверждена
        if (!Core.isPremiumPurchased) {
            Core.isPremiumPurchased = true

            Snackbar.make(findViewById(android.R.id.content), R.string.premium_purchased, Snackbar.LENGTH_LONG).show()

            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, Bundle().apply {
                putDouble(FirebaseAnalytics.Param.VALUE, PRICE.toDouble())
                putString(FirebaseAnalytics.Param.CURRENCY, USD.currencyCode)
            })

            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.PURCHASE, Bundle().apply {
                putDouble(FirebaseAnalytics.Param.VALUE, (70 percentOf PRICE).toDouble())
                putString(FirebaseAnalytics.Param.CURRENCY, USD.currencyCode)
                putString(FirebaseAnalytics.Param.ITEM_ID, Core.PREMIUM_SKU_ID)
                putString(FirebaseAnalytics.Param.ITEM_NAME, "Premium")
            })
        }
    }
}
