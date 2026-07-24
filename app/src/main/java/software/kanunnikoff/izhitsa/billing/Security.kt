package software.kanunnikoff.izhitsa.billing

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import java.io.IOException
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Проверяет подписи покупок открытым ключом приложения.
 *
 * Проверка на устройстве защищает от случайной подмены ответа, но не заменяет
 * серверную проверку в приложениях с ценными или постоянными покупками.
 */
object Security {
    private const val TAG = "IABUtil/Security"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    /**
     * Проверяет, что [signedData] подписаны ключом, парным к [base64PublicKey].
     *
     * @param base64PublicKey открытый ключ в кодировке Base64.
     * @param signedData подписанные данные покупки в исходном виде.
     * @param signature подпись данных в кодировке Base64.
     * @throws IOException если открытый ключ не удалось разобрать.
     */
    @Throws(IOException::class)
    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) || TextUtils.isEmpty(signature)) {
            Log.w(TAG, "Purchase verification failed: missing data.")
            return false
        }
        val key = generatePublicKey(base64PublicKey)
        return verify(key, signedData, signature)
    }

    /**
     * Создаёт [PublicKey] из строки Base64 в формате X.509.
     *
     * @param encodedPublicKey открытый ключ в кодировке Base64.
     * @throws IOException если спецификация ключа недействительна.
     */
    @Throws(IOException::class)
    fun generatePublicKey(encodedPublicKey: String): PublicKey {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            // RSA входит в обязательный набор криптографических алгоритмов Android.
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            val msg = "Invalid key specification: $e"
            Log.w(TAG, msg)
            throw IOException(msg)
        }
    }

    /**
     * Сверяет подпись [signature] с подписью, вычисленной для [signedData].
     *
     * @param publicKey открытый ключ учётной записи разработчика.
     * @param signedData исходные подписанные данные.
     * @param signature проверяемая подпись в Base64.
     * @return `true`, если данные подписаны закрытым ключом, парным к [publicKey].
     */
    fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes: ByteArray = try {
            Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Base64 decoding failed.")
            return false
        }
        try {
            val signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM)
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            if (!signatureAlgorithm.verify(signatureBytes)) {
                Log.w(TAG, "Signature verification failed.")
                return false
            }
            return true
        } catch (e: NoSuchAlgorithmException) {
            // RSA входит в обязательный набор криптографических алгоритмов Android.
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            Log.w(TAG, "Invalid key specification.")
        } catch (e: SignatureException) {
            Log.w(TAG, "Signature exception.")
        }
        return false
    }
}
