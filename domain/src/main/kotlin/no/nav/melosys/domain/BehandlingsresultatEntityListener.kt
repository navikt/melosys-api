package no.nav.melosys.domain

import mu.KotlinLogging
import jakarta.persistence.PostLoad
import jakarta.persistence.PreUpdate

class BehandlingsresultatEntityListener {
    private val log = KotlinLogging.logger {}

    @PreUpdate
    fun preUpdate(entity: Behandlingsresultat) {
        val stack = Thread.currentThread().stackTrace
            .drop(2).take(5)
            .joinToString("\n      ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
        log.info {
            """
            [JPA-PRE-UPDATE] Behandlingsresultat
              id: ${entity.id}
              type: ${entity.type}
              thread: ${Thread.currentThread().name}
              stack:
                $stack
            """.trimIndent()
        }
    }

    @PostLoad
    fun postLoad(entity: Behandlingsresultat) {
        log.info { "[JPA-POST-LOAD] Behandlingsresultat id=${entity.id} type=${entity.type} thread=${Thread.currentThread().name}" }
    }
}
