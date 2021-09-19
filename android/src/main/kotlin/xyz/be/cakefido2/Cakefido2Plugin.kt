package xyz.be.cakefido2

import android.content.Intent
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.be.cakefido2.api.MainViewModel
import xyz.be.cakefido2.api.SignInState
import kotlinx.coroutines.flow.collect

/** Cakefido2Plugin */
class Cakefido2Plugin : FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    companion object {
        private const val REQUEST_CODE_REGISTER = 1
        private const val REQUEST_CODE_SIGN = 2
    }

    private lateinit var channel: MethodChannel
    private lateinit var activity: FragmentActivity
    private lateinit var viewModel: MainViewModel
    private val lifecycleScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    var mFidoResult: Result? = null

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "test1")
        channel.setMethodCallHandler(this)
        viewModel = MainViewModel(binding.applicationContext)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        val map = call.arguments as? Map<*, *>
        mFidoResult = result
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "actionRegisterRequest" -> {
                val accessToken = map?.get("access_token") as? String
                registerRequest(accessToken ?: "")
            }
            "actionSetHeader" -> {
                Utils.getInstance().header = map?.get("header") as? HashMap<String, String>
                        ?: HashMap()
            }
            "actionSignInRequest" -> {
                val userName = map?.get("user_name") as? String
                viewModel.signinRequest(userName ?: "")
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun registerRequest(accessToken: String) {
        lifecycleScope.launch {
            val intent = viewModel.registerRequest(accessToken)
            if (intent != null) {
                activity.startIntentSenderForResult(intent.intentSender, REQUEST_CODE_REGISTER, Intent(), 0, 0, 0)
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity as FragmentActivity
        viewModel.setFido2ApiClient(Fido.getFido2ApiClient(activity))
        binding.addActivityResultListener(this)
        lifecycleScope.launch {
            viewModel.signinRequests.collect { intent ->
                activity.startIntentSenderForResult(intent.intentSender, REQUEST_CODE_SIGN, Intent(), 0, 0, 0)
            }
        }
        mainScope.launch {
            viewModel.signInState.collect { state ->
                when (state) {
                    is SignInState.SignedOut -> {
                    }
                    is SignInState.SigningIn -> {
                    }
                    is SignInState.SignInError -> {
                        mFidoResult?.success(state.data)
                    }
                    is SignInState.SignedIn -> {
                        mFidoResult?.success(state.data)
                    }
                    is SignInState.RegisterFailed -> {
                    }
                    is SignInState.RegisterPass -> {
                    }
                }
            }
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
        viewModel.setFido2ApiClient(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (data != null) {
            when (requestCode) {
                REQUEST_CODE_REGISTER -> handleCreateCredentialResult(data)
                else -> handleSignResult(data)
            }
            return true
        }
        return false
    }

    private fun handleCreateCredentialResult(data: Intent?) {
        val bytes = data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
//            activityResult.resultCode != Activity.RESULT_OK ->
//                Toast.makeText(this@MainActivity, "Cancel", Toast.LENGTH_LONG).show()
            bytes == null ->
                Toast.makeText(activity, "Error", Toast.LENGTH_LONG)
                        .show()
            else -> {
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    Toast.makeText(activity, response.errorMessage, Toast.LENGTH_LONG)
                            .show()
                } else {
                    viewModel.registerResponse(credential)
                }
            }
        }
    }

    private fun handleSignResult(data: Intent?) {
        val bytes = data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
//            activityResult.resultCode != Activity.RESULT_OK ->
//                Toast.makeText(this@MainActivity, "Cancel", Toast.LENGTH_LONG).show()
            bytes == null ->
                Toast.makeText(activity, "Error", Toast.LENGTH_LONG).show()
            else -> {
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    Toast.makeText(activity, response.errorMessage, Toast.LENGTH_LONG)
                            .show()
                } else {
                    viewModel.signinResponse(credential)
                }
            }
        }
    }
}