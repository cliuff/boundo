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

import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.provider.Provider

typealias LibDependency = Provider<out ModuleVersionSelector>

fun configLibCheckerRules(dependency: LibDependency, config: (version: String?) -> Unit) {
    dependency.getOrNull()?.run {
        // com.github.LibChecker:LibChecker-Rules-Bundle
        if (module.group != "com.github.LibChecker") return
        if (module.name != "LibChecker-Rules-Bundle") return
        config(version)
    }
}