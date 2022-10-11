package no.nav.melosys.melosysmock.oppgave

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import javax.annotation.PreDestroy

@Component
class OppgaveRepo(@Value("\${persist.repo.oppgave}") private val persist: Boolean) {
    companion object {
        private val log = LoggerFactory.getLogger(OppgaveRepo::class.java)
        private val fileName = "oppgave_repo.json"
    }

    val repo: MutableMap<Int, Oppgave> = load()

    fun finnSisteOppgaveId(): Int {
        return repo.keys.maxOrNull() ?: 1
    }

    private fun load(): MutableMap<Int, Oppgave> {
        val file = File(fileName)
        if (!file.exists()) return mutableMapOf()

        log.info("laster inn journalpost repo fra ${file.absolutePath}")
        val repoAsString = file.readText()
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        return mapper.readValue(repoAsString, object : TypeReference<MutableMap<Int, Oppgave>>() {})
    }

    @PreDestroy
    fun destroy() {
        if (!persist) return
        val repoAsString = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .writerWithDefaultPrettyPrinter().writeValueAsString(repo)
        log.info("lagrer OppgaveRepo til $fileName")
        File(fileName).printWriter().use { it.println(repoAsString) }
    }

}
