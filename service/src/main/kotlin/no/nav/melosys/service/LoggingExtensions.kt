package no.nav.melosys.service

import mu.KotlinLogging

private val log = KotlinLogging.logger {}

infix fun Boolean.logInfoIf(message: () -> String): Boolean = apply {
    if (this) log.info(message)
}
