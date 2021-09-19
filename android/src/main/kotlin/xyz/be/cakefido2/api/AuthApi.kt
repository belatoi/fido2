/*
 * Copyright 2019 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.be.cakefido2.api

import android.util.*
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import xyz.be.cakefido2.api.model.*
import java.io.StringReader
import java.io.StringWriter
import java.util.concurrent.TimeUnit

/**
 * Interacts with the server API.
 */
class AuthApi{

    private val client: OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(AddHeaderInterceptor())
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .build()

    companion object {
        private const val BASE_URL = "https://gw-cake.veep.tech/api/v1/bank-api/fido2"
        private val JSON = "application/json".toMediaTypeOrNull()
        private const val SessionIdKey = "webauthn-session="
        private const val TAG = "AuthApi"
    }

    /**
     * @param username The username to be used for sign-in.
     * @return The Session ID.
     */
    suspend fun username(username: String): ApiResult<Unit> {
        val call = client.newCall(
                Request.Builder()
                        .url("$BASE_URL/username")
                        .method("POST", jsonRequestBody {
                            name("username").value(username)
                        })
                        .build()
        )
        val response = call.await()
        return response.result("Error calling /username") { }
    }

    /**
     * @param sessionId The session ID received on `username()`.
     * @param password A password.
     * @return An [ApiResult].
     */
    suspend fun password(sessionId: String, password: String): ApiResult<Unit> {
        val call = client.newCall(
                Request.Builder()
                        .url("$BASE_URL/password")
                        .addHeader("Cookie", formatCookie(sessionId))
                        .method("POST", jsonRequestBody {
                            name("password").value(password)
                        })
                        .build()
        )
        val response = call.await()
        return response.result("Error calling /password") { }
    }

    /**
     * @param sessionId The session ID.
     * @return A list of all the credentials registered on the server.
     */
//    suspend fun getKeys(sessionId: String): ApiResult<List<Credential>> {
//        val call = client.newCall(
//                Request.Builder()
//                        .url("$BASE_URL/getKeys")
//                        .addHeader("Cookie", formatCookie(sessionId))
//                        .method("POST", jsonRequestBody {})
//                        .build()
//        )
//        val response = call.await()
//        return response.result("Error calling /getKeys") {
//            parseUserCredentials(body ?: throw ApiException("Empty response from /getKeys"))
//        }
//    }

    /**
     * @param sessionId The session ID.
     * @return A pair. The `first` element is an [PublicKeyCredentialCreationOptions] that can be
     * used for a subsequent FIDO2 API call. The `second` element is a challenge string that should
     * be sent back to the server in [registerResponse].
     */
    suspend fun registerRequest(accessToken: String): ApiResult<PublicKeyCredentialCreationOptions?> {
        val call = client.newCall(
                Request.Builder()
                        .url("$BASE_URL/register/begin")
                        .method("POST", jsonRequestBody {
                            name("access_token").value(accessToken)
                        })
                        .build()
        )
        val response = call.await()
        return response.result("Error calling /registerRequest") {
            parsePublicKeyCredentialCreationOptions(
                    body ?: throw ApiException("Empty response from /registerRequest")
            )
        }
    }

    /**
     * @param sessionId The session ID to be used for the sign-in.
     * @param credential The PublicKeyCredential object.
     * @return A list of all the credentials registered on the server, including the newly
     * registered one.
     */
    suspend fun registerResponse(
            sessionId: String,
            accessToken: String,
            credential: PublicKeyCredential
    ): ApiResult<String> {
        val rawId = credential.rawId.toBase64()
        val response = credential.response as AuthenticatorAttestationResponse

        val call = client.newCall(
                Request.Builder()
                        .url("$BASE_URL/register/finish")
                        .addHeader("cookie", formatCookie(sessionId))
                        .method("POST", jsonRequestBody {
                            name("id").value(rawId)
                            name("type").value(PublicKeyCredentialType.PUBLIC_KEY.toString())
                            name("rawId").value(rawId)
                            name("response").objectValue {
                                name("clientDataJSON").value(
                                        response.clientDataJSON.toBase64()
                                )
                                name("attestationObject").value(
                                        response.attestationObject.toBase64()
                                )
                            }
                            name("access_token").value(accessToken)
                        })
                        .build()
        )
        val apiResponse = call.await()
        return try {
            apiResponse.result("Error calling /registerResponse") {
                parseRegisterResponse(
                        body ?: throw ApiException("Empty response from /registerResponse")
                )
            }
        } catch (e: Exception) {
            ApiResult.Success("", "")
        }
    }

    /**
     * @param sessionId The session ID.
     * @param credentialId The credential ID to be removed.
     */
    suspend fun removeKey(sessionId: String, credentialId: String): ApiResult<Unit> {
        val call = client.newCall(
                Request.Builder()
                        .url("$BASE_URL/removeKey?credId=$credentialId")
                        .addHeader("Cookie", formatCookie(sessionId))
                        .method("POST", jsonRequestBody {})
                        .build()
        )
        val response = call.await()
        return response.result("Error calling /removeKey") { }
    }

    /**
     * @param sessionId The session ID to be used for the sign-in.
     * @param credentialId The credential ID of this device.
     * @return A pair. The `first` element is a [PublicKeyCredentialRequestOptions] that can be used
     * for a subsequent FIDO2 API call. The `second` element is a challenge string that should
     * be sent back to the server in [signinResponse].
     */
    suspend fun signinRequest(
            username: String
    ): ApiResult<PublicKeyCredentialRequestOptions?> {
        val call = client.newCall(
                Request.Builder()
                        .url("$BASE_URL/login/begin")
                        .method("POST", jsonRequestBody {
                            name("username").value(username)
                        })
                        .build()
        )
        val response = call.await()
        return response.result("Error calling /signinRequest") {
            parsePublicKeyCredentialRequestOptions(
                    body ?: throw ApiException("Empty response from /signinRequest")
            )
        }
    }

    /**
     * @param sessionId The session ID to be used for the sign-in.
     * @param credential The PublicKeyCredential object.
     * @return A list of all the credentials registered on the server, including the newly
     * registered one.
     */
    suspend fun signinResponse(
            sessionId: String,
            credential: PublicKeyCredential,
            username: String
    ): ApiResult<JSONObject> {
        val rawId = credential.rawId.toBase64()
        val response = credential.response as AuthenticatorAssertionResponse

        val call = client.newCall(
                Request.Builder()
                        .url("$BASE_URL/login/finish")
                        .addHeader("cookie", formatCookie(sessionId))
                        .method("POST", jsonRequestBody {
                            name("id").value(rawId)
                            name("type").value(PublicKeyCredentialType.PUBLIC_KEY.toString())
                            name("rawId").value(rawId)
                            name("username").value(username)
                            name("response").objectValue {
                                name("clientDataJSON").value(
                                        response.clientDataJSON.toBase64()
                                )
                                name("authenticatorData").value(
                                        response.authenticatorData.toBase64()
                                )
                                name("signature").value(
                                        response.signature.toBase64()
                                )
                                name("userHandle").value(
                                        response.userHandle?.toBase64() ?: ""
                                )
                            }
                        })
                        .build()
        )
        val apiResponse = call.await()
        return apiResponse.result("Error calling /signingResponse") {
            parseUserCredentials(body ?: throw ApiException("Empty response from /signinResponse"))
        }
    }

    private fun parsePublicKeyCredentialRequestOptions(
            body: ResponseBody
    ): PublicKeyCredentialRequestOptions? {
        val builder = PublicKeyCredentialRequestOptions.Builder()
        val jsonObject = JSONObject(body.string())
        if(jsonObject.has("publicKey")) {
            val json = jsonObject.getJSONObject("publicKey").toString()
            val loginBeginModel = Gson().fromJson(json,LoginBeginModel::class.java)
            builder.setChallenge(loginBeginModel.challenge.decodeBase64API())
            builder.setRpId(loginBeginModel.rpId)
            builder.setTimeoutSeconds(loginBeginModel.timeout)
            builder.setAllowList(parseCredentialDescriptors(loginBeginModel.allowCredentials))
            return builder.build()
        }
        return null
    }

    private fun parsePublicKeyCredentialCreationOptions(
            body: ResponseBody
    ): PublicKeyCredentialCreationOptions? {
        val builder = PublicKeyCredentialCreationOptions.Builder()
        val jsonObject = JSONObject(body.string())
        print("SADASDS $jsonObject")
        if(jsonObject.has("publicKey")) {
            val json = jsonObject.getJSONObject("publicKey").toString()
            val registerBeginModel = Gson().fromJson(json, RegisterBeginModel::class.java)
            builder.setChallenge(registerBeginModel.challenge.decodeBase64API())
            builder.setUser(parseUser(registerBeginModel.user))
            builder.setParameters(parseParameters(registerBeginModel.pubKeyCredParams))
            builder.setTimeoutSeconds(registerBeginModel.timeout)
            builder.setAuthenticatorSelection(parseSelection(registerBeginModel.authenticatorSelection))
            builder.setRp(parseRp(registerBeginModel.rp))
            return builder.build()
        }
        return null
    }

    private fun parseRp(rp: RpRegisterBeginModel): PublicKeyCredentialRpEntity {
        return PublicKeyCredentialRpEntity(rp.id, rp.name, /* icon */ null)
    }

    private fun parseSelection(authen: AuthenRegisterBeginModel): AuthenticatorSelectionCriteria {
        val builder = AuthenticatorSelectionCriteria.Builder()
        builder.setAttachment(Attachment.fromString(authen.authenticatorAttachment))
        return builder.build()
    }

    private fun parseCredentialDescriptors(
            listAllow : List<AllowCredentials>
    ): List<PublicKeyCredentialDescriptor> {
        val list = mutableListOf<PublicKeyCredentialDescriptor>()
        listAllow.forEach {
            list.add(
                    PublicKeyCredentialDescriptor(
                            PublicKeyCredentialType.PUBLIC_KEY.toString(),
                            it.id.decodeBase64API(),
                            /* transports */ null
                    )
            )
        }
        return list
    }

    private fun parseUser(user: UserRegisterBeginModel): PublicKeyCredentialUserEntity {
        return PublicKeyCredentialUserEntity(
                user.id.decodeBase64API(),
                user.name,
                null, // icon
                user.displayName
        )
    }

    private fun parseParameters(pubKeyCredParams: List<PubKeyRegisterBeginModel>): List<PublicKeyCredentialParameters> {
        val parameters = mutableListOf<PublicKeyCredentialParameters>()
        pubKeyCredParams.forEach {
            parameters.add(PublicKeyCredentialParameters(it.type, it.alg))
        }
        return parameters
    }

    private fun jsonRequestBody(body: JsonWriter.() -> Unit): RequestBody {
        val output = StringWriter()
        JsonWriter(output).use { writer ->
            writer.beginObject()
            writer.body()
            writer.endObject()
        }
        return output.toString().toRequestBody(JSON)
    }

    private fun parseRegisterResponse(body: ResponseBody): String {
        val jsonObject = JSONObject(body.string())
        if(jsonObject.has("code")) {
            if(jsonObject["code"] == 1) {
                return "success"
            }
        }
        return ""
    }

    private fun parseUserCredentials(body: ResponseBody): JSONObject {
        return JSONObject(body.string())
    }

    private fun throwResponseError(response: Response, message: String): Nothing {
        val b = response.body
        if (b != null) {
            throw ApiException("$message; ${parseError(b)}")
        } else {
            throw ApiException(message)
        }
    }

    private fun parseError(body: ResponseBody): String {
        val errorString = body.string()
        try {
            JsonReader(StringReader(errorString)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (name == "error") {
                        val token = reader.peek()
                        if (token == JsonToken.STRING) {
                            return reader.nextString()
                        }
                        return "Unknown"
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot parse the error: $errorString", e)
            // Don't throw; this method is called during throwing.
        }
        return ""
    }

    private fun JsonWriter.objectValue(body: JsonWriter.() -> Unit) {
        beginObject()
        body()
        endObject()
    }

    private fun <T> Response.result(errorMessage: String, data: Response.() -> T): ApiResult<T> {
        if (!isSuccessful) {
            if (code == 401) { // Unauthorized
                return ApiResult.SignedOutFromServer
            }
            // All other errors throw an exception.
            throwResponseError(this, errorMessage)
        }
        val cookie = headers("set-cookie").find { it.startsWith(SessionIdKey) }
        val sessionId = if (cookie != null) parseSessionId(cookie) else null
        return ApiResult.Success(sessionId, data())
    }

    private fun parseSessionId(cookie: String): String {
        val start = cookie.indexOf(SessionIdKey)
        if (start < 0) {
            throw ApiException("Cannot find $SessionIdKey")
        }
        val semicolon = cookie.indexOf(";", start + SessionIdKey.length)
        val end = if (semicolon < 0) cookie.length else semicolon
        return cookie.substring(start + SessionIdKey.length, end)
    }

    private fun formatCookie(sessionId: String): String {
        return "$SessionIdKey$sessionId"
    }
}
