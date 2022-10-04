package no.nav.melosys.melosysmock.person

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Component
class PersonRepoStorage(@Value("\${persist.repo.person}") private val persist: Boolean) {

    val repo = PersonRepo.repo

    companion object {
        private val log = LoggerFactory.getLogger(PersonRepoStorage::class.java)
        private val fileName = "person_repo.json"
    }

    fun finnVedIdent(ident: String): Person? = PersonRepo.finnVedIdent(ident)

    private fun load(): MutableMap<String, Person> {
        val file = File(fileName)
        if (!file.exists()) return mutableMapOf()

        log.info("laster inn $fileName fra ${file.absolutePath}")
        val repoAsString = file.readText()
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        return mapper.readValue(repoAsString, object : TypeReference<MutableMap<String, Person>>() {})
    }

    @PostConstruct
    fun init() {
        val map = load()
        if (map.isNotEmpty()) {
            PersonRepo.repo.clear()
            PersonRepo.repo.putAll(map)
        }
    }

    @PreDestroy
    fun destroy() {
        if (!persist) return
        val repoAsString = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .writerWithDefaultPrettyPrinter().writeValueAsString(repo)
        log.info("lagrer personRepo ${fileName}")
        File(fileName).printWriter().use { it.println(repoAsString) }
    }

}
