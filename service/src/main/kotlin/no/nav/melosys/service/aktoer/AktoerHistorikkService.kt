package no.nav.melosys.service.aktoer

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Representerer
import no.nav.melosys.repository.AuditRepository
import org.joda.time.LocalDate
import org.springframework.stereotype.Service

@Service
class AktoerHistorikkService(
    private val auditRepository: AuditRepository
) {

    fun hentAktørHistorikk(fagsak: Fagsak, rolle: Aktoersroller): List<AktoerHistorikk> {
        val revisions = auditRepository.getRevisions(Aktoer::class.java, mapOf("fagsak_saksnummer" to fagsak.saksnummer, "rolle" to rolle))

        return revisions.map {
            AktoerHistorikk(
                registrertFra = LocalDate.now(),
                registretTil = null,
                aktørId = it.entity.aktørId,
                personIdent = it.entity.personIdent,
                institusjonID = it.entity.institusjonId,
                orgnr = it.entity.orgnr,
                rolle = it.entity.rolle,
                representerer = it.entity.representerer,
                fullmakter = emptySet()
            )
        }
    }
}

data class AktoerHistorikk(
    val registrertFra: LocalDate,
    val registretTil: LocalDate? = null,
    val aktørId: String? = null,
    val personIdent: String? = null,
    val institusjonID: String? = null,
    val orgnr: String? = null,
    val rolle: Aktoersroller,
    val representerer: Representerer? = null,
    val fullmakter: Set<Fullmaktstype> = emptySet(),
)
