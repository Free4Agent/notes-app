package com.notes.app.data.remote

import io.ktor.client.engine.*

expect fun createHttpEngine(allowSelfSigned: Boolean): HttpClientEngine
