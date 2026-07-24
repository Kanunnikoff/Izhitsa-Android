package software.kanunnikoff.izhitsa.ui

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import software.kanunnikoff.izhitsa.AppPreferences

/**
 * Запрашивает встроенную оценку Google Play после нескольких запусков.
 *
 * @property activity окно, поверх которого Google Play может показать запрос.
 * @property preferences хранилище счётчика запусков и последней обработанной версии.
 */
class ReviewPrompter(
    private val activity: Activity,
    private val preferences: AppPreferences
) {
    /**
     * Выполняет не более одной попытки запроса для [currentVersion] после достижения порога.
     *
     * @param currentVersion текущий внутренний номер версии.
     */
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
