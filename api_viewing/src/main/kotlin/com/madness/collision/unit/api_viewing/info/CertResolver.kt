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

package com.madness.collision.unit.api_viewing.info

import android.content.Context
import android.content.pm.Signature
import com.madness.collision.unit.api_viewing.R
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

/** keyword : value : referenceName */
typealias PrincipalEntry = Triple<String, String, String?>
data class CertificateInfo(
    val cert: X509Certificate,
    val issuerValues: List<PrincipalEntry>,
    val subjectValues: List<PrincipalEntry>,
    val fingerprint: Triple<String, String, String>,
)

object CertResolver {
    @OptIn(ExperimentalStdlibApi::class)
    fun getCertificateInfo(signature: Signature, context: Context): CertificateInfo? {
        return try {
            val certBytes = signature.toByteArray()
            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(certBytes.inputStream()) as? X509Certificate
                ?: return null

            val hexFormat = HexFormat {
                upperCase = true
                bytes { byteSeparator = ":"; bytesPerGroup = 8; groupSeparator = "  " }
            }
            val digests = listOf("MD5", "SHA-1", "SHA-256")
                .map { MessageDigest.getInstance(it).digest(certBytes).toHexString(hexFormat) }
            val (issValues, subValues) = listOf(cert.issuerX500Principal, cert.subjectX500Principal)
                .map { convertPrincipal(it, context) }
            val fingerprint = Triple(digests[0], digests[1], digests[2])
            CertificateInfo(cert, issValues, subValues, fingerprint)
        } catch (e: CertificateException) {
            e.printStackTrace()
            null
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }

    fun convertPrincipal(principal: X500Principal, context: Context): List<PrincipalEntry> {
        return principal.toString().splitToSequence(", ")
            .map { keyVal -> keyVal.split('=') }
            .map { (keyword, value) -> Triple(keyword, value, getReferenceName(keyword, context)) }
            .toList()
    }

    private fun getReferenceName(keyword: String, context: Context): String? {
        return context.getString(when (keyword) {
            "CN" -> R.string.resPrincipalCN
            "OU" -> R.string.resPrincipalOU
            "O" -> R.string.resPrincipalO
            "L" -> R.string.resPrincipalL
            "ST" -> R.string.resPrincipalST
            "C" -> R.string.resPrincipalC
            "EMAILADDRESS" -> R.string.resPrincipalEmail
            else -> return null
        })
    }
}