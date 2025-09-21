/*
 * Copyright 2023 Clifford Liu
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

package com.cliuff.boundo.build

import org.gradle.api.Project
import java.util.Properties

class CustomConfig(val buildPackage: String, val signing: SigningConfig?)
class SigningConfig(val store: SigningKeyStore, val key: SigningKey)
class SigningKeyStore(val path: String, val password: String)
class SigningKey(val alias: String, val password: String)

// below: load the desired values from custom.properties in order to be injected into BuildConfig and Res
// the values in custom.properties must not contain quotes otherwise the parsed values here will contain quotes
fun getCustomConfig(project: Project): CustomConfig {
    val properties = Properties()
    try {
        val customPropFile = project.rootProject.file("doconfig/custom.properties")
        customPropFile.inputStream().use(properties::load)
    } catch (ignored: Exception) {
        return CustomConfig(buildPackage = "com.madness.collision", signing = null)
    }
    val prop: (key: String, defValue: String) -> String = properties::getProperty
    return CustomConfig(
        buildPackage = prop("packageName", "com.madness.collision"),
        signing = SigningConfig(
            store = SigningKeyStore(
                path = prop("signingKeyStorePath", ""),
                password = prop("signingKeyStorePassword", ""),
            ),
            key = SigningKey(
                alias = prop("signingKeyAlias", ""),
                password = prop("signingKeyPassword", ""),
            ),
        ),
    )
}
