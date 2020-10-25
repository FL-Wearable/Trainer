package fl.wearable.autosport.login

import androidx.lifecycle.ViewModel
import fl.wearable.autosport.BuildConfig.SYFT_AUTH_TOKEN

class LoginViewModel : ViewModel() {

    fun checkUrl(baseUrl: String): Boolean {
        return true
    }

    fun getAuthToken() : String = SYFT_AUTH_TOKEN
}