package me.kartikarora.klaat.core

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.docs.v1.Docs
import com.google.api.services.docs.v1.DocsScopes
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.awt.Desktop
import java.net.URI

class GoogleDocsClient {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val tokensDirectoryPath = System.getProperty("user.home") + "/.klaat/tokens"
    private val scopes = listOf(DocsScopes.DOCUMENTS_READONLY)

    private fun getCredentials(httpTransport: HttpTransport, retryCount: Int = 0): Credential {
        // Try to load credentials from bundled resource first, then fall back to file
        val clientSecrets = try {
            // Try to load from bundled resource in classpath
            val resourceStream = GoogleDocsClient::class.java.classLoader.getResourceAsStream("credentials/gcp_credentials.json")
            if (resourceStream != null) {
                resourceStream.reader().use { reader ->
                    GoogleClientSecrets.load(jsonFactory, reader)
                }
            } else {
                // Fall back to file in project root (for development)
                val projectRoot = System.getProperty("user.dir")
                val credentialsFile = File(projectRoot, "gcp_credentials.json")

                if (!credentialsFile.exists() || !credentialsFile.isFile) {
                    throw Exception("Unable to start authentication service. Please try again later.")
                }

                FileReader(credentialsFile).use { reader ->
                    GoogleClientSecrets.load(jsonFactory, reader)
                }
            }
        } catch (e: Exception) {
            throw Exception("Unable to start authentication service. Please try again later.", e)
        }

        val tokensDir = File(tokensDirectoryPath)
        val flow = try {
            GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, scopes
            )
                .setDataStoreFactory(FileDataStoreFactory(tokensDir))
                .setAccessType("offline")
                .build()
        } catch (e: Exception) {
            if (retryCount == 0) {
                println("⚠️  Stored credentials appear to be corrupted. Attempting re-authentication...")
                if (tokensDir.exists()) {
                    tokensDir.deleteRecursively()
                }
                return getCredentials(httpTransport, retryCount + 1)
            } else {
                throw Exception("Failed to initialize Google Docs client: ${e.message}", e)
            }
        }

        // 1. Try silent renewal first
        val credential = try {
            flow.loadCredential("user")
        } catch (e: Exception) {
            if (retryCount == 0) {
                println("⚠️  Failed to load stored credentials. Attempting re-authentication...")
                if (tokensDir.exists()) {
                    tokensDir.deleteRecursively()
                }
                return getCredentials(httpTransport, retryCount + 1)
            } else {
                throw Exception("Failed to load credentials: ${e.message}", e)
            }
        }

        if (credential != null) {
            val success = try {
                if (credential.expiresInSeconds != null && credential.expiresInSeconds <= 60) {
                    println("🔑 Token expired or expiring soon, renewing...")
                    credential.refreshToken()
                } else if (credential.accessToken == null) {
                    println("🔑 No access token found, refreshing...")
                    credential.refreshToken()
                } else {
                    true
                }
            } catch (e: Exception) {
                println("⚠️  Failed to refresh token: ${e.message}")
                false
            }

            if (success) {
                return credential
            }
        }

        // 2. Fallback to interactive flow if silent renewal fails or no credential exists
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()

        println("\n🔑 Opening browser for Google authentication...")
        println("   If browser doesn't open, visit the URL shown below\n")

        // Custom browser opener that doesn't use AWT
        val app = object : AuthorizationCodeInstalledApp(flow, receiver) {
            override fun onAuthorization(authorizationUrl: AuthorizationCodeRequestUrl) {
                // Use built-in build() method
                val urlString = authorizationUrl.build()

                println("Please open the following address in your browser:")
                println("  $urlString")
                println()

                // Try to open browser automatically - fallback to ProcessBuilder if AWT fails
                try {
                    // First try using ProcessBuilder (works in native image)
                    ProcessBuilder("open", urlString).start()
                    println("Opening browser...")
                } catch (e1: Exception) {
                    try {
                        // Fallback to Desktop.browse() if open command fails
                        if (Desktop.isDesktopSupported()) {
                            val desktop = Desktop.getDesktop()
                            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                                desktop.browse(URI(urlString))
                                println("Opening browser...")
                            }
                        }
                    } catch (e2: Exception) {
                        // Both methods failed, user will use the printed URL
                    }
                }

                println("Waiting for authorization code...")
            }
        }

        return app.authorize("user")
    }

    fun getDocsService(): Docs {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        return Docs.Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
            .setApplicationName("Klaat")
            .build()
    }
}