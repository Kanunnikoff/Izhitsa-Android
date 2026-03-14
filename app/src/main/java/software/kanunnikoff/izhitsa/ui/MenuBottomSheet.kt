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
        val firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        view.findViewById<TextView>(R.id.rateButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            startActivity(intent)
            
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, "app_rating")
                putString(FirebaseAnalytics.Param.ITEM_ID, packageName)
            })
            dismiss()
        }

        view.findViewById<TextView>(R.id.shareButton).setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Google Play: https://play.google.com/store/apps/details?id=$packageName")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, getString(R.string.app_name))
            startActivity(shareIntent)

            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, Bundle().apply {
                putString(FirebaseAnalytics.Param.CONTENT_TYPE, "link")
                putString(FirebaseAnalytics.Param.ITEM_ID, packageName)
            })
            dismiss()
        }

        view.findViewById<TextView>(R.id.otherAppsButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=9118553902079488918"))
            startActivity(intent)
            
            firebaseAnalytics.logEvent("developer_page_visited", null)
            dismiss()
        }

        view.findViewById<TextView>(R.id.translatorButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=software.kanunnikoff.yat"))
            startActivity(intent)
            
            firebaseAnalytics.logEvent("yat_page_visited", null)
            dismiss()
        }

        view.findViewById<TextView>(R.id.donateButton).setOnClickListener {
            if (!Core.isPremiumPurchased) {
                if (billingManager != null && billingManager.billingClientResponseCode > BillingManager.BILLING_MANAGER_NOT_INITIALIZED) {
                    billingManager.initiatePurchaseFlow(Core.PREMIUM_SKU_ID, BillingClient.ProductType.INAPP)

                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, Bundle().apply {
                        putDouble(FirebaseAnalytics.Param.VALUE, Core.PRICE.toDouble())
                        putString(FirebaseAnalytics.Param.CURRENCY, Core.USD.currencyCode)
                        putString(FirebaseAnalytics.Param.ITEM_NAME, "Premium")
                        putString(FirebaseAnalytics.Param.ITEM_ID, Core.PREMIUM_SKU_ID)
                    })
                }
            } else {
                Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.premium_already_purchased, Snackbar.LENGTH_LONG).show()
            }

            dismiss()
        }

        return view
    }

    companion object {
        const val TAG = "menu_bottom_sheet"
    }
}