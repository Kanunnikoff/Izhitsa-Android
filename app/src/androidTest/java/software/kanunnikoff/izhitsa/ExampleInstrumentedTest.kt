package software.kanunnikoff.izhitsa

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/** Проверяет базовую конфигурацию приложения на устройстве Android. */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    /** Контекст проверяемого приложения должен иметь ожидаемое имя пакета. */
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("software.kanunnikoff.izhitsa", appContext.packageName)
    }
}
