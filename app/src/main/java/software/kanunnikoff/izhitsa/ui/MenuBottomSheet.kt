package software.kanunnikoff.izhitsa.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.billingclient.api.BillingClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import software.kanunnikoff.izhitsa.Core
import software.kanunnikoff.izhitsa.R
import software.kanunnikoff.izhitsa.billing.BillingManager

class MenuBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.menu_bottom_sheet, null)

        val billingManager = (requireActivity() as? MainActivity)?.billingManager
        val packageName = requireActivity().packageName
        val analytics = FirebaseAnalytics.getInstance(requireContext())

        view.findViewById<TextView>(R.id.rateButton).setOnClickListener {
            openUrl("https://play.google.com/store/apps/details?id=$packageName")
            val rateParams = Bundle().apply {
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, "app")
                putString(FirebaseAnalytics.Param.ITEM_ID, packageName)
                putString(FirebaseAnalytics.Param.ITEM_NAME, "Rating of the app in Google Play.")
            }
            analytics.logEvent("rate_content", rateParams)
            dismiss()
        }

        view.findViewById<TextView>(R.id.shareButton).setOnClickListener {
            shareText(
                "Google Play: https://play.google.com/store/apps/details?id=$packageName",
                getString(R.string.app_name)
            )
            val shareParams = Bundle().apply {
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, "link")
                putString(FirebaseAnalytics.Param.ITEM_ID, packageName)
                putString(FirebaseAnalytics.Param.ITEM_NAME, "Link to the app in Google Play.")
            }
            analytics.logEvent(FirebaseAnalytics.Event.SHARE, shareParams)
            dismiss()
        }

        view.findViewById<TextView>(R.id.otherAppsButton).setOnClickListener {
            openUrl("https://play.google.com/store/apps/dev?id=9118553902079488918")
            analytics.logEvent("developer_page_opened", null)
            dismiss()
        }

        view.findViewById<TextView>(R.id.translatorButton).setOnClickListener {
            openUrl("https://play.google.com/store/apps/details?id=software.kanunnikoff.yat")
            analytics.logEvent("yat_page_opened", null)
            dismiss()
        }

        view.findViewById<TextView>(R.id.donateButton).setOnClickListener {
            if (!Core.isPremiumPurchased) {
                if (billingManager != null && billingManager.billingClientResponseCode > BillingManager.BILLING_MANAGER_NOT_INITIALIZED) {
                    billingManager.initiatePurchaseFlow(Core.PREMIUM_SKU_ID, BillingClient.ProductType.INAPP)
                    val cartParams = Bundle().apply {
                        putString(FirebaseAnalytics.Param.CURRENCY, Core.USD.currencyCode)
                        putDouble(FirebaseAnalytics.Param.VALUE, Core.PRICE.toDouble())
                        putString(FirebaseAnalytics.Param.ITEM_ID, Core.PREMIUM_SKU_ID)
                        putString(FirebaseAnalytics.Param.ITEM_NAME, "Premium")
                        putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "In-App Purchases")
                    }
                    analytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, cartParams)
                }
            } else {
                Snackbar.make(view, getString(R.string.premium_already_purchased), Snackbar.LENGTH_LONG).show()
            }

            dismiss()
        }

        return view
    }

    companion object {
        const val TAG = "menu_bottom_sheet"
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun shareText(text: String, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
