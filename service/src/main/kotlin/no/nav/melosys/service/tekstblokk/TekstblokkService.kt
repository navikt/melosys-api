package no.nav.melosys.service.tekstblokk

import java.util.Locale

import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
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
        val tags: List<String>?,
    )

    @Transactional(readOnly = true)
    fun hentAlleOversikter(type: TekstblokkType?): List<TekstblokkOversikt> {
        val oversikter = tekstblokkRepository.finnOversikt(type)
        if (oversikter.isEmpty()) return oversikter

        val tagsPerId = tekstblokkRepository.finnTagsForIds(oversikter.map { it.id })
            .groupBy({ it[0] as Long }, { it[1] as String })
        oversikter.forEach { it.tags = tagsPerId[it.id]?.toSet() ?: emptySet() }
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
        tekstblokk.tags.clear()
        tekstblokk.tags.addAll(normaliserTags(input.tags))
    }

    private fun normaliserTags(tags: List<String>?): List<String> =
        tags
            ?.asSequence()
            // Bevar bokstavstørrelse (f.eks. "USA-avtale") og tillat mellomrom i tags.
            // Vi trimmer kun ytterkanter og slår sammen gjentatt blanktegn til ett.
            ?.map { it.trim().replace(FLERE_BLANKTEGN, " ") }
            ?.filter { it.isNotBlank() }
            // Unngå nær-duplikater som kun skiller seg i bokstavstørrelse; behold første variant.
            ?.distinctBy { it.lowercase(Locale.ROOT) }
            ?.toList()
            ?: emptyList()

    private companion object {
        private val FLERE_BLANKTEGN = Regex("\\s+")
    }
}
