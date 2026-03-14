package software.kanunnikoff.izhitsa.billing

import com.android.billingclient.api.Purchase
import software.kanunnikoff.izhitsa.Core
import software.kanunnikoff.izhitsa.ui.MainActivity

/**
 * Handles control logic of the BaseGamePlayActivity
 */
class MainViewController(private val activity: MainActivity) {
    val updateListener = UpdateListener()

    // Tracks if we currently own a premium
    private var isPremium = false

    fun isPremiumPurchased(): Boolean = isPremium

    /**
     * Handler to billing updates
     */
    inner class UpdateListener : BillingManager.BillingUpdatesListener {
        override fun onBillingClientSetupFinished() {
            activity.onBillingManagerSetupFinished()
        }

        override fun onPurchasesUpdated(purchases: List<Purchase>) {
            for (purchase in purchases) {
                for (product in purchase.products) {
                    if (Core.PREMIUM_SKU_ID == product) {
                        isPremium = true
                        activity.premiumPurchased()
                        break
                    }
                }
            }
        }
    }
}
