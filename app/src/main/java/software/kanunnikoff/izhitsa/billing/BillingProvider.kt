package software.kanunnikoff.izhitsa.billing

/**
 * An interface that provides an access to BillingLibrary methods
 */
interface BillingProvider {
    fun isPremiumPurchased(): Boolean
}
