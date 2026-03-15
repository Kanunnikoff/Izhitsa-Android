package software.kanunnikoff.izhitsa.billing

import com.android.billingclient.api.Purchase
import software.kanunnikoff.izhitsa.Core
import software.kanunnikoff.izhitsa.ui.MainActivity

/**
 * Handles control logic of the BaseGamePlayActivity
 */
class MainViewController(private val mActivity: MainActivity) {
    val updateListener: UpdateListener = UpdateListener()

    // Tracks if we currently own a premium
    var isPremiumPurchased = false
        private set

    /**
     * Handler to billing updates
     */
    inner class UpdateListener : BillingManager.BillingUpdatesListener {
        override fun onBillingClientSetupFinished() {
            mActivity.onBillingManagerSetupFinished()
        }

        override fun onPurchasesUpdated(purchases: List<Purchase>) {
            for (purchase in purchases) {
                if (purchase.products.contains(Core.PREMIUM_SKU_ID)) {
                    isPremiumPurchased = true
                    mActivity.premiumPurchased()
                }
            }
        }
    }
}
