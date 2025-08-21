package no.nav.melosys.domain

fun Any.readResourceAsString(path: String): String =
    javaClass.classLoader.getResource(path)
        ?.readText()
        ?: error("Resource not found: $path")
