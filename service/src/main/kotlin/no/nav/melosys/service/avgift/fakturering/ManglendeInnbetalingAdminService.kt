package no.nav.melosys.service.avgift.fakturering

import jakarta.persistence.EntityManager
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.saksflytapi.domain.LåsReferanseType
import no.nav.melosys.saksflytapi.domain.ManglendeInnbetalingBehandlingLåsReferanse
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties
import java.util.UUID

private const val TIDSGRENSE_SEKUNDER = 10

@Service
class ManglendeInnbetalingAdminService(private val entityManager: EntityManager) {

    /**
     * `behandlingsresultat.fakturaserie_referanse` har ingen indeks, så dette er én full skanning av tabellen
     * uansett hvor mange referanser som slås opp. Derfor ett kall for hele lista og ikke ett per referanse.
     */
    @Transactional(readOnly = true, timeout = TIDSGRENSE_SEKUNDER)
    fun hentSaker(fakturaserieReferanser: List<String>): List<FakturaserieSakDto> {
        val sakerPerReferanse = entityManager.createQuery(
            """
            SELECT br.fakturaserieReferanse, f.saksnummer, f.status
            FROM Behandlingsresultat br JOIN br.behandling b JOIN b.fagsak f
            WHERE br.fakturaserieReferanse IN :referanser
            ORDER BY br.registrertDato DESC, b.id DESC
            """,
            Array<Any>::class.java
        )
            .setParameter("referanser", fakturaserieReferanser)
            .resultList
            .groupBy({ it[0] as String }, { Sak(it[1] as String, it[2] as Saksstatuser) })

        return fakturaserieReferanser.map { referanse ->
            val saker = sakerPerReferanse[referanse].orEmpty()
            val nyeste = saker.firstOrNull()
            FakturaserieSakDto(
                fakturaserieReferanse = referanse,
                saksnummer = nyeste?.saksnummer,
                saksstatus = nyeste?.status?.name,
                antallSaker = saker.map { it.saksnummer }.distinct().size
            )
        }
    }

    /**
     * Filtrerer på eksakt prosesstype, så Oracle kan bruke indeksen på (prosess_type, status). Bare varselbrevene
     * mangler låsreferanse, så prosessdataene (CLOB) leses bare for dem.
     */
    @Transactional(readOnly = true, timeout = TIDSGRENSE_SEKUNDER)
    fun hentProsesser(): List<ManglendeInnbetalingProsessDto> {
        val fakturanummerForVarselbrev = entityManager.createQuery(
            "SELECT p.id, p.data FROM Prosessinstans p WHERE p.type = :type",
            Array<Any?>::class.java
        )
            .setParameter("type", ProsessType.MANGLENDE_INNBETALING_VARSELBREV)
            .resultList
            .associate { it[0] as UUID to (it[1] as Properties?)?.getProperty(ProsessDataKey.FAKTURANUMMER.kode) }

        return entityManager.createQuery(
            """
            SELECT p.id, p.type, p.status, p.registrertDato, p.låsReferanse, f.saksnummer
            FROM Prosessinstans p LEFT JOIN p.behandling b LEFT JOIN b.fagsak f
            WHERE p.type IN :typer
            ORDER BY p.registrertDato, p.id
            """,
            Array<Any?>::class.java
        )
            .setParameter(
                "typer",
                listOf(ProsessType.MANGLENDE_INNBETALING_VARSELBREV, ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING)
            )
            .resultList
            .map { rad ->
                val type = rad[1] as ProsessType
                val lås = (rad[4] as String?)
                    ?.takeIf { LåsReferanseType.UBETALT.erGyldigReferanse(it) }
                    ?.let { ManglendeInnbetalingBehandlingLåsReferanse(it) }
                val fakturanummer = when (type) {
                    ProsessType.MANGLENDE_INNBETALING_VARSELBREV -> fakturanummerForVarselbrev[rad[0]]
                    else -> lås?.fakturanummer
                }
                ManglendeInnbetalingProsessDto(
                    fakturanummer = fakturanummer,
                    fakturaserieReferanse = lås?.fakturaserieReferanse,
                    saksnummer = rad[5] as String?,
                    prosesstype = type.name,
                    status = (rad[2] as ProsessStatus).name,
                    // Skrevet med LocalDateTime.now() i JVM-sonen (Europe/Oslo i prod)
                    registrert = (rad[3] as LocalDateTime).atZone(ZoneId.systemDefault()).toInstant()
                )
            }
    }

    private data class Sak(val saksnummer: String, val status: Saksstatuser)
}
