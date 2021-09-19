package xyz.be.cakefido2.api.model

import com.google.gson.annotations.SerializedName

data class RegisterBeginModel(
        @SerializedName("challenge")
        val challenge: String = "",
        @SerializedName("timeout")
        val timeout: Double = 60000.0,
        @SerializedName("rp")
        val rp: RpRegisterBeginModel = RpRegisterBeginModel(),
        @SerializedName("user")
        val user: UserRegisterBeginModel = UserRegisterBeginModel(),
        @SerializedName("pubKeyCredParams")
        val pubKeyCredParams: List<PubKeyRegisterBeginModel> = ArrayList(),
        @SerializedName("authenticatorSelection")
        val authenticatorSelection: AuthenRegisterBeginModel = AuthenRegisterBeginModel()
)

data class RpRegisterBeginModel(
        @SerializedName("name")
        val name: String = "",
        @SerializedName("id")
        val id: String = "",
)

data class UserRegisterBeginModel(
        @SerializedName("name")
        val name: String = "",
        @SerializedName("displayName")
        val displayName: String = "",
        @SerializedName("id")
        val id: String = "",
)

data class PubKeyRegisterBeginModel(
        @SerializedName("type")
        val type: String = "",
        @SerializedName("alg")
        val alg: Int = 0,
)

data class AuthenRegisterBeginModel(
        @SerializedName("authenticatorAttachment")
        val authenticatorAttachment: String = "",
        @SerializedName("userVerification")
        val userVerification: String = "",
)


