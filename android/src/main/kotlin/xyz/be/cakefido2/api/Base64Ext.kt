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

import android.util.Base64
import java.net.URLDecoder

private const val BASE64_FLAG = Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING

fun ByteArray.toBase64API(): String {
    return Base64.encodeToString(this, Base64.DEFAULT)
}

fun String.decodeBase64API(): ByteArray {
    return Base64.decode(this, Base64.DEFAULT)
}

fun ByteArray.toBase64(): String {
    return Base64.encodeToString(this, BASE64_FLAG)
}

fun String.decodeBase64(): ByteArray {
    return Base64.decode(this, BASE64_FLAG)
}
