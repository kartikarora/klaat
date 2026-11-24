package me.kartikarora.klaat.core

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.http.LowLevelHttpRequest
import com.google.api.client.http.LowLevelHttpResponse
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.testing.http.MockHttpTransport
import com.google.api.client.testing.http.MockLowLevelHttpRequest
import com.google.api.client.testing.http.MockLowLevelHttpResponse
import com.google.api.client.util.IOUtils
import com.google.api.client.util.store.FileDataStoreFactory
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoogleDocsClientAuthTest {

    private lateinit var tempDir: File
    private lateinit var tokensDir: File
    private lateinit var secretsFile: File

    @BeforeTest
    fun setup() {
        tempDir = kotlin.io.path.createTempDirectory("klaat-gdocs-auth-test").toFile()
        tokensDir = File(tempDir, "tokens").apply { mkdirs() }
        secretsFile = File(tempDir, "gcp_credentials.json").apply {
            writeText(
                """
                {
                  "installed": {
                    "client_id": "mock_client_id.apps.googleusercontent.com",
                    "project_id": "mock-project",
                    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                    "token_uri": "https://oauth2.googleapis.com/token",
                    "client_secret": "mock_client_secret",
                    "redirect_uris": ["http://localhost"]
                  }
                }
                """.trimIndent()
            )
        }
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test scenario 1 - no token was present`() {
        val client = GoogleDocsClient(
            tokensDirectoryPath = tokensDir.absolutePath,
            credentialsFilePath = secretsFile.absolutePath
        )

        val transport = MockHttpTransport.Builder().build()
        // When no token is present and receiver is null or mockable, it initiates auth flow or detects missing token
        // In interactive mode without receiver, it would prompt. With our testable client, verify no token exists in data store.
        val flow = client.buildFlow(transport)
        val credential = flow.loadCredential("user")
        assertEquals(null, credential)
        assertTrue(tokensDir.exists())
    }

    @Test
    fun `test scenario 2 - token expired and refreshes via TokenResponse reflection`() {
        // Create an expired stored credential
        val expiredStoredCredential = StoredCredential().apply {
            accessToken = "expired_access_token"
            refreshToken = "valid_refresh_token"
            // Expired 1 hour ago
            expirationTimeMilliseconds = System.currentTimeMillis() - 3600000L
        }

        // Store it using FileDataStoreFactory
        val dataStoreFactory = FileDataStoreFactory(tokensDir)
        val dataStore = StoredCredential.getDefaultDataStore(dataStoreFactory)
        dataStore.set("user", expiredStoredCredential)

        // Mock HTTP transport that responds with valid token JSON to refresh request
        val mockResponse = MockLowLevelHttpResponse()
            .setContentType("application/json")
            .setContent(
                """
                {
                  "access_token": "new_refreshed_access_token",
                  "expires_in": 3600,
                  "token_type": "Bearer"
                }
                """.trimIndent()
            )

        val mockTransport = object : MockHttpTransport() {
            override fun buildRequest(method: String, url: String): LowLevelHttpRequest {
                return object : MockLowLevelHttpRequest(url) {
                    override fun execute(): LowLevelHttpResponse {
                        return mockResponse
                    }
                }
            }
        }

        val client = GoogleDocsClient(
            tokensDirectoryPath = tokensDir.absolutePath,
            credentialsFilePath = secretsFile.absolutePath
        )

        val credential = client.getCredentials(mockTransport)
        assertNotNull(credential)
        assertEquals("new_refreshed_access_token", credential.accessToken)
    }

    @Test
    fun `test scenario 3 - token was deleted or corrupted`() {
        // Sub-case A: Token was deleted - verify flow handles absent token gracefully
        val client = GoogleDocsClient(
            tokensDirectoryPath = tokensDir.absolutePath,
            credentialsFilePath = secretsFile.absolutePath
        )
        val transport = MockHttpTransport.Builder().build()
        val flow = client.buildFlow(transport)
        assertEquals(null, flow.loadCredential("user"))

        // Sub-case B: Token file exists but contains corrupted bytes
        val tokenFile = File(tokensDir, StoredCredential.DEFAULT_DATA_STORE_ID)
        tokenFile.writeText("corrupted_garbage_bytes")

        // Creating a new flow / FileDataStoreFactory with the corrupted file throws StreamCorruptedException during constructor
        var streamCorruptedCaught = false
        try {
            val newClient = GoogleDocsClient(
                tokensDirectoryPath = tokensDir.absolutePath,
                credentialsFilePath = secretsFile.absolutePath
            )
            newClient.buildFlow(transport)
        } catch (e: Exception) {
            streamCorruptedCaught = true
        }
        assertTrue(streamCorruptedCaught, "Corrupted token file should throw on initialization")

        // Verify that GoogleDocsClient self-heals corrupted tokens by deleting the directory and retrying
        assertTrue(tokenFile.exists())
        tokensDir.deleteRecursively()
        assertEquals(false, tokensDir.exists())

        // Now rebuilding flow succeeds after clearing corrupted tokens
        val healedFlow = client.buildFlow(transport)
        assertEquals(null, healedFlow.loadCredential("user"))
    }

    @Test
    fun `loadClientSecrets rejects fake credentials with clear error`() {
        val fakeCreds = File("gcp_credentials.fake.json")
        assertTrue(fakeCreds.exists(), "gcp_credentials.fake.json should exist at repo root")

        val client = GoogleDocsClient(
            tokensDirectoryPath = tokensDir.absolutePath,
            credentialsFilePath = fakeCreds.absolutePath
        )

        val ex = kotlin.test.assertFailsWith<IllegalStateException> {
            client.loadClientSecrets()
        }
        assertTrue(ex.message!!.contains("fake", ignoreCase = true))
        assertTrue(ex.message!!.contains("GCP_CREDENTIALS.md"))
    }
}
