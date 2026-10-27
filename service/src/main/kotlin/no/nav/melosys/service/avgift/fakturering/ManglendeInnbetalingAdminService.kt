package no.nav.melosys.service.avgift.fakturering

import jakarta.persistence.EntityManager
import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.saksflytapi.domain.LåsReferanseType
import no.nav.melosys.saksflytapi.domain.ManglendeInnbetalingBehandlingLåsReferanse
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.hibernate.query.NativeQuery
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.StringReader
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties
import java.util.UUID

private val log = KotlinLogging.logger { }

private const val TIDSGRENSE_SEKUNDER = 10

// Oracle tillater maks 1000 verdier i én IN-liste
private const val MAKS_BINDINGER_I_IN = 1000

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
     * Filtrerer på eksakt prosesstype, så Oracle kan bruke indeksen på (prosess_type, status). Prosessdataene (CLOB)
     * leses bare for varselbrevene, som mangler låsreferanse, og for nye behandlinger der låsreferansen mangler eller
     * ikke kan tolkes (f.eks. fra før låsen ble innført, eller fakturanummer som ikke er rene tall).
     */
    @Transactional(readOnly = true, timeout = TIDSGRENSE_SEKUNDER)
    fun hentProsesser(): List<ManglendeInnbetalingProsessDto> {
        val prosesser = entityManager.createQuery(
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
                Prosess(
                    id = rad[0] as UUID,
                    type = type,
                    status = rad[2] as ProsessStatus,
                    registrert = rad[3] as LocalDateTime,
                    lås = if (type == ProsessType.MANGLENDE_INNBETALING_VARSELBREV) null else tolkLås(rad[4] as String?),
                    saksnummer = rad[5] as String?
                )
            }

        val nyeUtenLås = prosesser
            .filter { it.type == ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING && it.lås == null }
            .map { it.id }
        val prosessdata = buildMap {
            putAll(lesProsessdata("prosess_type = :verdi", ProsessType.MANGLENDE_INNBETALING_VARSELBREV.name))
            nyeUtenLås.chunked(MAKS_BINDINGER_I_IN).forEach { putAll(lesProsessdata("uuid IN (:verdi)", it)) }
        }

        return prosesser.map { prosess ->
            val data = prosessdata[prosess.id]
            ManglendeInnbetalingProsessDto(
                fakturanummer = prosess.lås?.fakturanummer ?: data?.getProperty(ProsessDataKey.FAKTURANUMMER.kode),
                fakturaserieReferanse = prosess.lås?.fakturaserieReferanse
                    ?: data?.getProperty(ProsessDataKey.FAKTURASERIE_REFERANSE.kode),
                saksnummer = prosess.saksnummer,
                prosesstype = prosess.type.name,
                status = prosess.status.name,
                // Skrevet med LocalDateTime.now() i JVM-sonen (Europe/Oslo i prod)
                registrert = prosess.registrert.atZone(ZoneId.systemDefault()).toInstant()
            )
        }
    }

    private fun tolkLås(låsReferanse: String?): ManglendeInnbetalingBehandlingLåsReferanse? = låsReferanse
        ?.takeIf { LåsReferanseType.UBETALT.erGyldigReferanse(it) && it.split("_").size == 3 }
        ?.let { ManglendeInnbetalingBehandlingLåsReferanse(it) }

    /**
     * Leser `data` som tekst og tolker hver rad for seg, så én prosess med ødelagte data bare mister sine egne felt
     * i stedet for å velte hele svaret (som PropertiesConverter på entiteten ville gjort).
     */
    private fun lesProsessdata(betingelse: String, verdi: Any): Map<UUID, Properties?> =
        entityManager.createNativeQuery("SELECT uuid AS id, data AS data FROM prosessinstans WHERE $betingelse")
            .setParameter("verdi", verdi)
            .unwrap(NativeQuery::class.java)
            .addScalar("id", UUID::class.java)
            .addScalar("data", String::class.java)
            .resultList
            .associate { rad ->
                val (id, data) = (rad as Array<*>).let { it[0] as UUID to it[1] as String? }
                id to tolkProsessdata(id, data)
            }

    private fun tolkProsessdata(id: UUID, data: String?): Properties? = try {
        data?.let { Properties().apply { load(StringReader(it)) } }
    } catch (e: Exception) {
        log.warn { "Kunne ikke tolke prosessdata for prosess $id: ${e.javaClass.simpleName}" }
        null
    }

    private data class Prosess(
        val id: UUID,
        val type: ProsessType,
        val status: ProsessStatus,
        val registrert: LocalDateTime,
        val lås: ManglendeInnbetalingBehandlingLåsReferanse?,
        val saksnummer: String?
    )

    private data class Sak(val saksnummer: String, val status: Saksstatuser)
}
