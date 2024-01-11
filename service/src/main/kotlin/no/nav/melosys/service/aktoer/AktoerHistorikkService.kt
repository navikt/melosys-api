package no.nav.melosys.service.aktoer

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.repository.AuditRepository
import no.nav.melosys.repository.EntityRevision
import org.hibernate.envers.RevisionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime

@Service
class AktoerHistorikkService(
    private val auditRepository: AuditRepository
) {

    @Transactional(readOnly = true)
    fun hentGyldigeAktørerPåTidspunkt(fagsak: Fagsak, rolle: Aktoersroller, tidspunkt: Instant): List<Aktoer> {
        val revisions: List<EntityRevision<Aktoer>> =
            auditRepository.getRevisionsBeforeOrAtDate(Aktoer::class.java, mapOf("fagsak_saksnummer" to fagsak.saksnummer, "rolle" to rolle), tidspunkt)

        return revisions.groupBy { it.entity.id }
            .mapNotNull { (_, revisionList) -> revisionList.maxByOrNull { it.revisionInfo.timestamp }}
            .filter { it.revisionType != RevisionType.DEL }
            .map { it.entity }
    }

    @Transactional(readOnly = true)
    fun hentAktørHistorikk(fagsak: Fagsak, rolle: Aktoersroller): List<AktoerHistorikk> {
        val revisions: List<EntityRevision<Aktoer>> =
            auditRepository.getRevisions(Aktoer::class.java, mapOf("fagsak_saksnummer" to fagsak.saksnummer, "rolle" to rolle))

        return lagHistorikk(revisions)
    }

    private fun lagHistorikk(revisions: List<EntityRevision<Aktoer>>): List<AktoerHistorikk> {
        val sortedRevisions = revisions.sortedBy { it.revisionInfo.timestamp }
        val revisionAndNextPairs = sortedRevisions.mapIndexed { index, revision ->
            val nextRevision = sortedRevisions.drop(index + 1).firstOrNull { it.entity.id == revision.entity.id }
            revision to nextRevision
        }

        return revisionAndNextPairs.filter { (revision, _) -> revision.revisionType != RevisionType.DEL }
            .map { (revision, next) ->
                AktoerHistorikk(
                    registrertFra = revision.revisionLocalDateTime,
                    registretTil = next?.revisionLocalDateTime,
                    aktørId = revision.entity.aktørId,
                    personIdent = revision.entity.personIdent,
                    institusjonID = revision.entity.institusjonId,
                    orgnr = revision.entity.orgnr,
                    rolle = revision.entity.rolle,
                    fullmakter = revision.entity.fullmakter.map { it.type }.toSet()
                )
        }
    }
}

data class AktoerHistorikk(
    val registrertFra: LocalDateTime,
    val registretTil: LocalDateTime?,
    val aktørId: String? = null,
    val personIdent: String? = null,
    val institusjonID: String? = null,
    val orgnr: String? = null,
    val rolle: Aktoersroller,
    val fullmakter: Set<Fullmaktstype>,
)
