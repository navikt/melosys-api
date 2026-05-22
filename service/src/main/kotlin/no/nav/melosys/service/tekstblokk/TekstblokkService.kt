package no.nav.melosys.service.tekstblokk

import java.util.Locale

import no.nav.melosys.domain.tekstblokk.Tekstblokk
import no.nav.melosys.domain.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.tekstblokk.TekstblokkType
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TekstblokkService(
    private val tekstblokkRepository: TekstblokkRepository,
    private val htmlSanitizer: TekstblokkHtmlSanitizer,
) {

    data class Input(
        val tittel: String,
        val innhold: String,
        val type: TekstblokkType,
        val tags: Collection<String>?,
    )

    @Transactional(readOnly = true)
    fun hentAlleOversikter(type: TekstblokkType?): List<TekstblokkOversikt> {
        val oversikter = tekstblokkRepository.finnOversikt(type)
        if (oversikter.isEmpty()) return oversikter

        val tagsPerId: Map<Long, MutableSet<String>> = oversikter.associate { it.id to mutableSetOf<String>() }
        tekstblokkRepository.finnTagsForIds(tagsPerId.keys).forEach { rad ->
            val id = rad[0] as Long
            val tag = rad[1] as String
            tagsPerId[id]?.add(tag)
        }
        oversikter.forEach { it.tags = tagsPerId[it.id] ?: emptySet() }
        return oversikter
    }

    @Transactional(readOnly = true)
    fun hent(id: Long): Tekstblokk = tekstblokkRepository.findById(id)
        .orElseThrow { IkkeFunnetException("Finner ikke tekstblokk med id $id") }

    @Transactional
    fun opprett(input: Input): Tekstblokk {
        val tekstblokk = Tekstblokk()
        populerFraInput(tekstblokk, input)
        return tekstblokkRepository.save(tekstblokk)
    }

    @Transactional
    fun oppdater(id: Long, input: Input): Tekstblokk {
        val tekstblokk = hent(id)
        populerFraInput(tekstblokk, input)
        return tekstblokkRepository.save(tekstblokk)
    }

    @Transactional
    fun slett(id: Long) {
        tekstblokkRepository.delete(hent(id))
    }

    /**
     * Atomisk batch-opprettelse. Brukes fra melosys-console for å seede inn mange
     * blokker samtidig. Enten lagres alle, eller ingen (én transaksjon).
     */
    @Transactional
    fun opprettBulk(inputs: List<Input>): List<Tekstblokk> = inputs.map { input ->
        val tekstblokk = Tekstblokk()
        populerFraInput(tekstblokk, input)
        tekstblokkRepository.save(tekstblokk)
    }

    private fun populerFraInput(tekstblokk: Tekstblokk, input: Input) {
        tekstblokk.tittel = input.tittel.trim()
        tekstblokk.innhold = htmlSanitizer.saniter(input.innhold) ?: ""
        tekstblokk.type = input.type
        tekstblokk.tags = normaliserTags(input.tags).toMutableSet()
    }

    private fun normaliserTags(tags: Collection<String>?): Set<String> =
        tags
            ?.asSequence()
            ?.filter { it.isNotBlank() }
            ?.map { it.trim().lowercase(Locale.ROOT) }
            ?.toSet()
            ?: emptySet()
}
