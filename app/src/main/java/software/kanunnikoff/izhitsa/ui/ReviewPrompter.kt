package software.kanunnikoff.izhitsa.ui

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import software.kanunnikoff.izhitsa.AppPreferences

class ReviewPrompter(
    private val activity: Activity,
    private val preferences: AppPreferences
) {
    fun requestIfNeeded(currentVersion: String) {
        preferences.launchesCount += 1

        if (
            preferences.launchesCount < LaunchThreshold ||
            preferences.lastReviewVersion == currentVersion
        ) {
            return
        }

        val reviewManager = ReviewManagerFactory.create(activity)

        reviewManager.requestReviewFlow().addOnCompleteListener { request ->
            if (!request.isSuccessful) {
                return@addOnCompleteListener
            }

            reviewManager.launchReviewFlow(activity, request.result)
                .addOnCompleteListener {
                    /*
                     * Google Play намеренно не сообщает, показалось ли окно и
                     * оставил ли пользователь оценку. После завершения попытки
                     * текущую версию больше не беспокоим повторным запросом.
                     */
                    preferences.lastReviewVersion = currentVersion
                }
        }
    }

    companion object {
        private const val LaunchThreshold = 5
    }
}
