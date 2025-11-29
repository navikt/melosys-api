package no.nav.melosys.domain

import mu.KotlinLogging
import jakarta.persistence.PostLoad
import jakarta.persistence.PreUpdate

class BehandlingsresultatEntityListener {
    private val log = KotlinLogging.logger {}

    @PreUpdate
    fun preUpdate(entity: Behandlingsresultat) {
        val stackFrames = Thread.currentThread().stackTrace.drop(2).take(12)
        val stack = stackFrames.joinToString("\n      ") { "${it.className}.${it.methodName}:${it.lineNumber}" }

        // Detect if this update is coming from LovvalgsperiodeService (should NOT happen after fix)
        val fromLovvalgsperiodeService = stackFrames.any {
            it.className.contains("LovvalgsperiodeService")
        }

        val marker = if (fromLovvalgsperiodeService) "[RACE-CONDITION-SUSPECT]" else ""

        log.warn {
            """
            [JPA-PRE-UPDATE] $marker Behandlingsresultat
              id: ${entity.id}
              type: ${entity.type}
              thread: ${Thread.currentThread().name}
              fromLovvalgsperiodeService: $fromLovvalgsperiodeService
              stack:
                $stack
            """.trimIndent()
        }
    }

    @PostLoad
    fun postLoad(entity: Behandlingsresultat) {
        log.debug { "[JPA-POST-LOAD] Behandlingsresultat id=${entity.id} type=${entity.type} thread=${Thread.currentThread().name}" }
    }
}
