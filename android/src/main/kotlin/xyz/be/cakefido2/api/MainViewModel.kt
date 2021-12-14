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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import xyz.be.cakefido2.Cakefido2Plugin
import java.lang.ref.WeakReference

class MainViewModel constructor(context: Context) : ViewModel() {

    private val repository: AuthRepository = AuthRepository(context)
    val signInState = repository.signInState
    private val signinRequestChannel = Channel<PendingIntent>(capacity = Channel.CONFLATED)
    val signinRequests = signinRequestChannel.receiveAsFlow()

    fun setFido2ApiClient(client: Fido2ApiClient?) {
        repository.setFido2APiClient(client)
    }

    suspend fun registerRequest(accessToken: String): PendingIntent? {
        try {
            repository.addAccessToken(accessToken)
            return repository.registerRequest()
        } finally {
        }
    }

    fun registerResponse(credential: PublicKeyCredential) {
        viewModelScope.launch {
            try {
                repository.registerResponse(credential)
            } finally {
            }
        }
    }

    suspend fun signinRequest(userName: String): PendingIntent? {
        repository.addUsername(userName)
        return repository.signinRequest()
    }

    fun signinResponse(credential: PublicKeyCredential) {
        viewModelScope.launch {
            try {
                repository.signinResponse(credential)
            } finally {
            }
        }
    }
}
