package no.nav.melosys.service.tekstblokk

import java.time.Instant
import java.time.LocalDateTime

import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.repository.AuditRepository
import no.nav.melosys.repository.EntityRevision
import org.hibernate.envers.RevisionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TekstblokkHistorikkService(
    private val auditRepository: AuditRepository,
) {

    @Transactional(readOnly = true)
    fun hentHistorikk(id: Long): List<TekstblokkVersjon> =
        lagVersjoner(auditRepository.getRevisions(Tekstblokk::class.java, mapOf("id" to id)))

    /**
     * «Hva sa blokken da vedtaket ble fattet». Returnerer null hvis blokken ikke fantes
     * – eller var slettet – på tidspunktet.
     */
    @Transactional(readOnly = true)
    fun hentVersjonPaaTidspunkt(id: Long, tidspunkt: Instant): TekstblokkVersjon? =
        lagVersjoner(auditRepository.getRevisionsBeforeOrAtDate(Tekstblokk::class.java, mapOf("id" to id), tidspunkt))
            .lastOrNull()
            ?.takeIf { it.endringstype != Endringstype.SLETTET }

    private fun lagVersjoner(revisjoner: List<EntityRevision<Tekstblokk>>): List<TekstblokkVersjon> {
        // Revisjonsnummeret er monotont; timestamp har millisekundoppløsning og kan kollidere
        val kronologisk = revisjoner.sortedBy { it.revisionInfo.id }
        return kronologisk.mapIndexed { indeks, revisjon ->
            TekstblokkVersjon(
                // Versjonsnummeret er per blokk, ikke globalt: revisjonsnummeret i Envers
                // deles med alle andre auditerte entiteter.
                versjon = indeks + 1,
                gyldigFra = revisjon.revisionLocalDateTime,
                gyldigTil = kronologisk.getOrNull(indeks + 1)?.revisionLocalDateTime,
                endretAv = revisjon.entity.endretAv,
                endretAvNavn = revisjon.entity.endretAvNavn,
                endringstype = endringstype(revisjon),
                tittel = revisjon.entity.tittel,
                innhold = revisjon.entity.innhold,
            )
        }
    }

    // Sletting er soft delete, så den kommer som en MOD-revisjon med slettetDato satt.
    private fun endringstype(revisjon: EntityRevision<Tekstblokk>): Endringstype = when {
        revisjon.revisionType == RevisionType.DEL || revisjon.entity.slettetDato != null -> Endringstype.SLETTET
        revisjon.revisionType == RevisionType.ADD -> Endringstype.OPPRETTET
        else -> Endringstype.ENDRET
    }
}

data class TekstblokkVersjon(
    val versjon: Int,
    val gyldigFra: LocalDateTime,
    val gyldigTil: LocalDateTime?,
    val endretAv: String,
    val endretAvNavn: String?,
    val endringstype: Endringstype,
    val tittel: String,
    val innhold: String,
)

enum class Endringstype {
    OPPRETTET,
    ENDRET,
    SLETTET
}
