package xyz.be.cakefido2

class Utils private constructor() {
    var header = HashMap<String, String>()
    var environment = ""

    val url: String
        get() {
            return when (environment) {
                "PROD" -> "https://gw.cake.vn/api/v1"
                "STAG" -> "https://gw-cake-new-2.veep.me/api/v1"
                "QA" -> "https://gw-cake-qa-new.veep.me/api/v1"
                "DEV" -> "https://gw-cake.veep.tech/api/v1"
                else -> "https://gw.cake.vn/api/v1"
            }
        }

    companion object Singleton {
        private var single: Utils? = null
        fun getInstance(): Utils {
            if (single == null) {
                single = Utils()
            }
            return single as Utils
        }
    }
}
