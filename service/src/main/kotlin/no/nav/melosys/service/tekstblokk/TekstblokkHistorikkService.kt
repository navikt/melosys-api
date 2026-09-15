package no.nav.melosys.service.tekstblokk

import java.time.Instant
import java.time.LocalDateTime

import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkStatus
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
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
        // Tidsstempelet er kronologien. Revisjonsnummeret er IKKE monotont i tid i drift:
        // historikk fra prod viser «Opprettet» midt i lista og tidsrom som løper bakover,
        // fordi revinfo-numrene kommer ut av rekkefølge. Nummeret beholdes kun som
        // tiebreaker: timestamp har millisekundoppløsning, og to lagringer rett etter
        // hverandre lander gjerne på samme millisekund.
        val kronologisk = revisjoner.sortedWith(compareBy({ it.revisionInfo.timestamp }, { it.revisionInfo.id }))
        return kronologisk.mapIndexed { indeks, revisjon ->
            val blokk = revisjon.entity
            // Nullbar her, men ikke på entiteten: aud-rader fra før V167 mangler status
            val status: TekstblokkStatus? = blokk.status
            TekstblokkVersjon(
                // Versjonsnummeret er per blokk, ikke globalt: revisjonsnummeret i Envers
                // deles med alle andre auditerte entiteter.
                versjon = indeks + 1,
                gyldigFra = revisjon.revisionLocalDateTime,
                gyldigTil = kronologisk.getOrNull(indeks + 1)?.revisionLocalDateTime,
                endretAv = blokk.endretAv,
                endretAvNavn = blokk.endretAvNavn,
                endringstype = endringstype(revisjon),
                tittel = blokk.tittel,
                innhold = blokk.innhold,
                // Envers slår opp collections i _aud-tabellene først ved traversering – kopien må tas i transaksjonen
                tags = blokk.tags.toList(),
                sakstyper = blokk.sakstyper.toList(),
                sakstemaer = blokk.sakstemaer.toList(),
                behandlingstemaer = blokk.behandlingstemaer.toList(),
                status = status ?: TekstblokkStatus.PUBLISERT,
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
    val tags: List<String>,
    val sakstyper: List<Sakstyper>,
    val sakstemaer: List<Sakstemaer>,
    val behandlingstemaer: List<Behandlingstema>,
    val status: TekstblokkStatus,
)

enum class Endringstype {
    OPPRETTET,
    ENDRET,
    SLETTET
}
