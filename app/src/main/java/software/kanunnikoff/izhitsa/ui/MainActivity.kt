package software.kanunnikoff.izhitsa.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.annotation.UiThread
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.*
import com.google.firebase.analytics.FirebaseAnalytics
import io.fabric.sdk.android.Fabric
import org.jetbrains.anko.*
import org.jetbrains.anko.design.longSnackbar
import software.kanunnikoff.izhitsa.Core
import software.kanunnikoff.izhitsa.Core.PRICE
import software.kanunnikoff.izhitsa.Core.USD
import software.kanunnikoff.izhitsa.R
import software.kanunnikoff.izhitsa.billing.BillingManager
import software.kanunnikoff.izhitsa.billing.BillingProvider
import software.kanunnikoff.izhitsa.billing.MainViewController
import software.kanunnikoff.izhitsa.percentOf

class MainActivity : AppCompatActivity(), AnkoLogger, BillingProvider {
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

        Fabric.with(this, Crashlytics())
    }

    override fun isPremiumPurchased(): Boolean {
        return viewController!!.isPremiumPurchased
    }

    fun onBillingManagerSetupFinished() {
        debug("In-App Billing client is configured")
    }

    @UiThread
    fun premiumPurchased() {  // покупка подтверждена
        if (!Core.isPremiumPurchased) {
            Core.isPremiumPurchased = true

            contentView?.longSnackbar(getString(R.string.premium_purchased))

            Answers.getInstance().logStartCheckout(
                StartCheckoutEvent()
                    .putTotalPrice(PRICE)
                    .putCurrency(USD)
                    .putItemCount(1))

            Answers.getInstance().logPurchase(
                PurchaseEvent()
                    .putItemPrice(70 percentOf PRICE)
                    .putCurrency(USD)
                    .putItemName("Premium")
                    .putItemType("In-App Purchases")
                    .putItemId(Core.PREMIUM_SKU_ID)
                    .putSuccess(true))
        }
    }
}
