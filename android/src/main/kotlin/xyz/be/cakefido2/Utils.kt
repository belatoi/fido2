package xyz.be.cakefido2

class Utils private constructor() {
    var header = HashMap<String, String>()

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
