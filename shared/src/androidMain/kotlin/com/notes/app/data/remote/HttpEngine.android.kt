package com.notes.app.data.remote

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import java.security.cert.X509Certificate
import javax.net.ssl.*

actual fun createHttpEngine(allowSelfSigned: Boolean): HttpClientEngine {
    return OkHttp.create {
        if (allowSelfSigned) {
            val unsafeTrustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            
            val sslContext = SSLContext.getInstance("SSL").apply {
                init(null, arrayOf(unsafeTrustManager), java.security.SecureRandom())
            }
            
            config {
                sslSocketFactory(sslContext.socketFactory, unsafeTrustManager)
                hostnameVerifier { _, _ -> true }
            }
        }
    }
}
