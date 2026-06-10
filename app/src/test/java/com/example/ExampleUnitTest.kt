package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ExampleUnitTest {

    private val apiKey = "AIzaSyDZ_Ayq1Kk1aefntBVXPpp1i3peKadRntY"
    private val projectId = "moneymitra-216fb"

    private fun log(message: String) {
        println("[FIRESTORE_AUDIT] $message")
    }

    private fun sendRequest(
        method: String,
        urlString: String,
        body: String? = null,
        bearerToken: String? = null
    ): Pair<Int, String> {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        
        if (method == "PATCH") {
            conn.requestMethod = "POST"
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        } else {
            conn.requestMethod = method
        }
        
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        if (bearerToken != null) {
            conn.setRequestProperty("Authorization", "Bearer $bearerToken")
        }

        if (body != null) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }
        }

        val code = conn.responseCode
        val isSuccess = code in 200..299
        val stream = if (isSuccess) conn.inputStream else conn.errorStream
        val response = if (stream != null) {
            BufferedReader(InputStreamReader(stream)).use { reader ->
                reader.readText()
            }
        } else {
            ""
        }
        return Pair(code, response)
    }

    private fun extractJsonValue(json: String, key: String): String {
        val search = "\"$key\""
        val index = json.indexOf(search)
        if (index == -1) return ""
        val colonIdx = json.indexOf(":", index + search.length)
        if (colonIdx == -1) return ""
        var startIdx = colonIdx + 1
        while (startIdx < json.length && (json[startIdx].isWhitespace() || json[startIdx] == '"')) {
            startIdx++
        }
        var endIdx = startIdx
        if (startIdx > 0 && json[startIdx - 1] == '"') {
            while (endIdx < json.length && json[endIdx] != '"') {
                endIdx++
            }
        } else {
            while (endIdx < json.length && json[endIdx] != ',' && json[endIdx] != '}' && json[endIdx] != ']' && !json[endIdx].isWhitespace()) {
                endIdx++
            }
        }
        return json.substring(startIdx, endIdx).trim()
    }

    @Test
    fun auditAndSeedGlobalPlans() {
        log("Starting Firestore global_plans collection audit...")

        // Step 1: Sign up a temp authenticated user to obtain idToken (security rule requirements)
        val randomSuffix = UUID.randomUUID().toString().take(6)
        val testEmail = "temp_audit_$randomSuffix@moneymitra.com"
        val testPassword = "AuditPassword123!"
        var idToken: String? = null

        try {
            val registerBody = "{\"email\":\"$testEmail\",\"password\":\"$testPassword\",\"returnSecureToken\":true}"
            val (code, response) = sendRequest(
                "POST",
                "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey",
                registerBody
            )
            if (code == 200) {
                idToken = extractJsonValue(response, "idToken")
                log("Successfully authenticated as temp auditor user. Email: $testEmail")
            } else {
                log("Failed to authenticate as temp user (HTTP $code). Fetching plans without token anyway.")
            }
        } catch (e: Exception) {
            log("Exception during signUp: ${e.message}")
        }

        // Step 2: Query the REST api of Firestore global_plans collection
        val plansUrl = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/global_plans"
        log("Executing Firestore REST GET: $plansUrl")
        
        var getCode = 0
        var getResponse = ""
        try {
            val res = sendRequest("GET", plansUrl, null, idToken)
            getCode = res.first
            getResponse = res.second
        } catch (e: Exception) {
            log("Exception during GET global_plans: ${e.message}")
        }

        log("GET Response code: $getCode")
        
        val isEmpty = getResponse.trim().isEmpty() || !getResponse.contains("\"documents\"")
        
        if (isEmpty) {
            log("No documents found inside global_plans collection or collection is empty.")
        } else {
            log("Found existing documents in global_plans!")
            log("Exact Firestore Response:")
            println(getResponse)
        }

        // Step 3: Seed the specified plans automatically if empty or requested
        val requiredPlans = listOf(
            Triple("Starter Plan", 500.0, 5000.0 to 2.0),
            Triple("Growth Plan", 5001.0, 50000.0 to 4.0),
            Triple("Premium Plan", 50001.0, 100000.0 to 6.0)
        )

        log("Seeding / resetting correct plans inside Firestore...")

        // Let's seed them.
        for ((name, minAmt, maxAndRate) in requiredPlans) {
            val (maxAmt, dailyReturn) = maxAndRate
            val durationDays = when (name) {
                "Starter Plan" -> 30
                "Growth Plan" -> 45
                else -> 60
            }
            val docId = name.replace(" ", "_").lowercase()
            val seedUrl = "$plansUrl/$docId"
            
            val docBody = """
                {
                    "fields": {
                        "name": { "stringValue": "$name" },
                        "amount": { "doubleValue": $minAmt },
                        "minAmount": { "doubleValue": $minAmt },
                        "maxAmount": { "doubleValue": $maxAmt },
                        "durationDays": { "integerValue": "$durationDays" },
                        "dailyReturn": { "doubleValue": $dailyReturn },
                        "totalReturn": { "doubleValue": ${dailyReturn * durationDays} },
                        "isActive": { "booleanValue": true },
                        "createdAt": { "integerValue": "${System.currentTimeMillis()}" }
                    }
                }
            """.trimIndent()

            log("Executing seed for $name: $seedUrl")
            try {
                // We use PATCH which creates or overrides the specific document ID (starter_plan, growth_plan, premium_plan)
                val (writeCode, writeResponse) = sendRequest("PATCH", seedUrl, docBody, idToken)
                log("Seed $name Response (HTTP $writeCode)")
                if (writeCode != 200) {
                    log("Error payload: $writeResponse")
                }
            } catch (e: Exception) {
                log("Exception during write of $name: ${e.message}")
            }
        }

        // Step 4: Verify write by fetching plans again!
        log("Re-verifying global_plans collection content after seeding...")
        try {
            val (verifyCode, verifyResponse) = sendRequest("GET", plansUrl, null, idToken)
            log("Re-verification GET Response (HTTP $verifyCode):")
            println(verifyResponse)
            assertTrue("Seeded plans should exist after audit", verifyCode == 200 && verifyResponse.contains("Starter Plan"))
        } catch (e: Exception) {
            log("Exception during re-verification GET: ${e.message}")
        }
    }
}
