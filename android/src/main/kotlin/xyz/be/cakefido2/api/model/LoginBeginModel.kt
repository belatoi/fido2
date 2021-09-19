package xyz.be.cakefido2.api.model

import com.google.gson.annotations.SerializedName

data class LoginBeginModel(
        @SerializedName("challenge")
        val challenge: String = "",
        @SerializedName("timeout")
        val timeout: Double = 60000.0,
        @SerializedName("rpId")
        val rpId: String = "",
        @SerializedName("allowCredentials")
        val allowCredentials: List<AllowCredentials> = ArrayList()
)

data class AllowCredentials(
        @SerializedName("type")
        val type: String = "",
        @SerializedName("id")
        val id: String = "",
)