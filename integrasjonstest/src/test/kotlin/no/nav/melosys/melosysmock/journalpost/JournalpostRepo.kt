package no.nav.melosys.melosysmock.journalpost

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.melosysmock.journalpost.intern_modell.JournalpostModell
import org.springframework.stereotype.Component
import java.io.File
import javax.annotation.PreDestroy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

@Component
class JournalpostRepo(@Value("\${persist.repo.journalpost}") private val persist: Boolean) {
    companion object {
        private val log = LoggerFactory.getLogger(JournalpostRepo::class.java)
        private val fileName = "journalpost_repo.json"
    }

    val repo: MutableMap<String, JournalpostModell> = load()

    private fun load(): MutableMap<String, JournalpostModell> {
        val file = File(fileName)
        if (!file.exists()) return mutableMapOf()

        log.info("laster inn $fileName fra ${file.absolutePath}")
        val repoAsString = file.readText()
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        return mapper.readValue(repoAsString, object : TypeReference<MutableMap<String, JournalpostModell>>() {})
    }

    @PreDestroy
    fun destroy() {
        if (!persist) return
        val repoAsString = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .writerWithDefaultPrettyPrinter().writeValueAsString(repo)
        log.info("lagrer $fileName")
        File(fileName).printWriter().use { it.println(repoAsString) }
    }

    fun finnVedSaksnummer(saksnummer: String) = repo
        .map { it.value }
        .filter { it.sakId == saksnummer }

    fun add(journalpostModell: JournalpostModell) {
        repo[journalpostModell.journalpostId] = journalpostModell
    }
}
