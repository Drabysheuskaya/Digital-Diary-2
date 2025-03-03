import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.darina.PRM_2_S25580.R

import androidx.biometric.BiometricPrompt.*
import java.util.concurrent.Executor

class Authentication : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo
    private lateinit var keyguardManager: KeyguardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.authentication)

        executor = ContextCompat.getMainExecutor(this)
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        biometricPrompt = BiometricPrompt(this, executor, object : AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                showToast("Biometric authentication error: $errString")
            }

            override fun onAuthenticationSucceeded(result: AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                startEntityActivity() // Start EntityActivity after successful authentication
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                showToast("Biometric authentication failed")
            }
        })

        val promptInfoBuilder = PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Authenticate using your biometrics")
            .setDescription("Use your fingerprint or face to authenticate")

        if (keyguardManager.isKeyguardSecure) {
            promptInfoBuilder.setDeviceCredentialAllowed(true)
        } else {
            showToast("Device PIN/Pattern/Password is not set up.")
            finish()
        }

        promptInfo = promptInfoBuilder.build()
    }

    override fun onResume() {
        super.onResume()
        authenticateUser()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun startEntityActivity() {
        val intent = Intent(this@Authentication, EntityActivity::class.java)
        startActivity(intent)
    }

    private fun authenticateUser() {
        val biometricManager = BiometricManager.from(this)
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Use fallback authentication method (PIN, Pattern, Password)
                biometricPrompt.authenticate(promptInfo)
            }
        }
    }
}
