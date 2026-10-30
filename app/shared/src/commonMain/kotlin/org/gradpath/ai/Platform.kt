package org.gradpath.ai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform