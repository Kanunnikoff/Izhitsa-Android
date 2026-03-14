package software.kanunnikoff.izhitsa.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.billingclient.api.BillingClient
import com.crashlytics.android.answers.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.browse
import org.jetbrains.anko.support.v4.share
import software.kanunnikoff.izhitsa.Core
import software.kanunnikoff.izhitsa.R
import software.kanunnikoff.izhitsa.billing.BillingManager

class MenuBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.menu_bottom_sheet, null)

        val billingManager = (requireActivity() as? MainActivity)?.billingManager
        val packageName = requireActivity().packageName

        view.findViewById<TextView>(R.id.rateButton).setOnClickListener {
            browse("https://play.google.com/store/apps/details?id=$packageName")
            Answers.getInstance().logRating(
                RatingEvent()
                    .putContentName("Rating of the app in Google Play.")
                    .putContentType("app")
                    .putContentId(packageName))
            dismiss()
        }

        view.findViewById<TextView>(R.id.shareButton).setOnClickListener {
            share("Google Play: https://play.google.com/store/apps/details?id=$packageName", getString(R.string.app_name))
            Answers.getInstance().logShare(
                ShareEvent()
                    .putContentName("Link to the app in Google Play.")
                    .putContentType("link")
                    .putContentId(packageName))
            dismiss()
        }

        view.findViewById<TextView>(R.id.otherAppsButton).setOnClickListener {
            browse("https://play.google.com/store/apps/dev?id=9118553902079488918")
            Answers.getInstance().logCustom(CustomEvent("Developer's page visited."))
            dismiss()
        }

        view.findViewById<TextView>(R.id.translatorButton).setOnClickListener {
            browse("https://play.google.com/store/apps/details?id=software.kanunnikoff.yat")
            Answers.getInstance().logCustom(CustomEvent("Yat's page visited."))
            dismiss()
        }

        view.findViewById<TextView>(R.id.donateButton).setOnClickListener {
            if (!Core.isPremiumPurchased) {
                if (billingManager != null && billingManager.billingClientResponseCode > BillingManager.BILLING_MANAGER_NOT_INITIALIZED) {
                    billingManager.initiatePurchaseFlow(Core.PREMIUM_SKU_ID, BillingClient.SkuType.INAPP)

                    Answers.getInstance().logAddToCart(
                        AddToCartEvent()
                            .putItemPrice(Core.PRICE)
                            .putCurrency(Core.USD)
                            .putItemName("Premium")
                            .putItemType("In-App Purchases")
                            .putItemId(Core.PREMIUM_SKU_ID))
                }
            } else {
                view.longSnackbar(getString(R.string.premium_already_purchased))
            }

            dismiss()
        }

        return view
    }

    companion object {
        const val TAG = "menu_bottom_sheet"
    }
}