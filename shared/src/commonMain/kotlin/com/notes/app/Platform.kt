package com.notes.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
