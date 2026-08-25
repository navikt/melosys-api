package no.nav.melosys.itest

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.vedtakMetadata
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflyt.statistikk.RammeavtaleSak
import no.nav.melosys.saksflyt.statistikk.RammeavtaleStatistikkService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.forTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Verifiserer mot ekte Oracle at uttrekket for rammeavtale om fjernarbeid (TWA, MELOSYS-8150) kun teller
 * ferdigbehandlede saker (fastsatt lovvalg med vedtaksdato), at året som telles er vedtaksåret, og at
 * saksnummeret (MEL-nr) bak hver behandling blir med i responsen.
 */
class RammeavtaleStatistikkIT(
    @Autowired private val rammeavtaleStatistikkService: RammeavtaleStatistikkService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
) : ComponentTestBase() {

    @Test
    fun `teller kun ferdigbehandlede saker med fjernarbeid, gruppert paa vedtaksaar`() {
        lagSak("MEL-8150-A", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 3, 1))
        lagSak("MEL-8150-B", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2024, 6, 1))
        // Anmodning sendt, men svar ikke mottatt/lovvalg ikke fastsatt -> ingen vedtaksdato, skal ikke telles
        lagSak("MEL-8150-C", erFjernarbeid = true, resultattype = Behandlingsresultattyper.ANMODNING_OM_UNNTAK, vedtaksdato = null)
        // Ferdigbehandlet, men ikke huket av for rammeavtale -> skal ikke telles
        lagSak("MEL-8150-D", erFjernarbeid = false, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 5, 1))
        // Fastsatt lovvalg, men uten vedtaksdato -> skal ikke telles
        lagSak("MEL-8150-E", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = null)
        // Annullert i ettertid (vedtaksdatoen består) -> skal ikke telles
        lagSak("MEL-8150-K", erFjernarbeid = true, resultattype = Behandlingsresultattyper.ANNULLERT, vedtaksdato = dato(2025, 6, 1))
        // Fjernarbeid-flagget satt på en annen prosesstype enn anmodning om unntak -> skal ikke telles
        val behandlingIdFeilProsesstype = lagSak(
            "MEL-8150-L",
            erFjernarbeid = false,
            resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
            vedtaksdato = dato(2025, 7, 1),
        )
        lagProsess(behandlingIdFeilProsesstype, erFjernarbeid = true, prosessType = ProsessType.SEND_BREV)

        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null)

        withClue("Kun A (2025) og B (2024) er ferdigbehandlet med fjernarbeid huket av på anmodningen") {
            statistikk.antallPerVedtaksaar shouldBe mapOf("2024" to 1L, "2025" to 1L)
            statistikk.antall shouldBe 2
        }
        withClue("Saksnummer følger med, sortert på vedtaksdato") {
            statistikk.saker shouldBe listOf(
                RammeavtaleSak("MEL-8150-B", "2024", LocalDate.of(2024, 6, 1)),
                RammeavtaleSak("MEL-8150-A", "2025", LocalDate.of(2025, 3, 1)),
            )
        }
    }

    @Test
    fun `fom og tom filtrerer paa vedtaksdato og tom er inklusiv`() {
        lagSak("MEL-8150-F", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2024, 12, 31))
        lagSak("MEL-8150-G", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 1, 1))
        lagSak("MEL-8150-H", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 12, 31))
        lagSak("MEL-8150-I", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2026, 1, 1))

        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 12, 31),
        )

        withClue("Både 1. januar og 31. desember skal være med, men ikke dagene utenfor") {
            statistikk.antallPerVedtaksaar shouldBe mapOf("2025" to 2L)
            statistikk.antall shouldBe 2
            statistikk.saker!!.map { it.saksnummer } shouldBe listOf("MEL-8150-G", "MEL-8150-H")
        }
    }

    @Test
    fun `flere anmodninger paa samme behandling telles kun en gang`() {
        val behandlingId = lagSak(
            "MEL-8150-J",
            erFjernarbeid = true,
            resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
            vedtaksdato = dato(2025, 2, 1),
        )
        lagProsess(behandlingId, erFjernarbeid = true)

        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null)

        statistikk.antallPerVedtaksaar shouldBe mapOf("2025" to 1L)
        withClue("DISTINCT skal fjerne duplikatraden fra den andre anmodningen") {
            statistikk.saker shouldBe listOf(RammeavtaleSak("MEL-8150-J", "2025", LocalDate.of(2025, 2, 1)))
        }
    }

    @Test
    fun `to behandlinger paa samme sak gir to rader med samme saksnummer`() {
        val fagsak = opprettFagsak("MEL-8150-M")
        // De to første deler vedtaksdato: uten br.behandling_id i select-lista ville SELECT DISTINCT slått dem
        // sammen til én rad, og antallet ville blitt 3 i stedet for 4
        lagBehandling(fagsak, erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 4, 1))
        lagBehandling(fagsak, erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 4, 1))
        lagBehandling(fagsak, erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 8, 1))
        // Annen sak samme dato: uten saksnummer i ORDER BY er rekkefølgen mellom denne og MEL-8150-M vilkårlig
        lagSak("MEL-8150-A2", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 4, 1))

        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null)

        withClue("Statistikken teller behandlinger, ikke saker, så MEL-nummeret gjentas") {
            statistikk.antall shouldBe 4
            statistikk.antallPerVedtaksaar shouldBe mapOf("2025" to 4L)
            statistikk.saker shouldBe listOf(
                RammeavtaleSak("MEL-8150-A2", "2025", LocalDate.of(2025, 4, 1)),
                RammeavtaleSak("MEL-8150-M", "2025", LocalDate.of(2025, 4, 1)),
                RammeavtaleSak("MEL-8150-M", "2025", LocalDate.of(2025, 4, 1)),
                RammeavtaleSak("MEL-8150-M", "2025", LocalDate.of(2025, 8, 1)),
            )
        }
    }

    @Test
    fun `vedtak rett foer nyttaar UTC telles i vedtaksaaret etter norsk lokaltid`() {
        // hibernate.timezone.default_storage=NORMALIZE lagrer Instant som veggklokke i JVM-tidssonen, så
        // vedtaksåret følger norsk lokaltid. Dette er dokumentert i repository-KDoc-en, og er utestet uten dette
        lagSak(
            "MEL-8150-TZ",
            erFjernarbeid = true,
            resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
            vedtaksdato = Instant.parse("2024-12-31T23:00:00Z"),
        )

        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null)

        withClue("23:00Z 31. desember er 00:00 1. januar i Norge, og skal telles i 2025") {
            statistikk.antallPerVedtaksaar shouldBe mapOf("2025" to 1L)
            statistikk.saker shouldBe listOf(RammeavtaleSak("MEL-8150-TZ", "2025", LocalDate.of(2025, 1, 1)))
        }
    }

    @Test
    fun `inkluderSaksnummer false gir tallene uten saksnummerlisten`() {
        lagSak("MEL-8150-N", erFjernarbeid = true, resultattype = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, vedtaksdato = dato(2025, 3, 1))

        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(
            null,
            null,
            inkluderSaksnummer = false,
        )

        statistikk.saker.shouldBeNull()
        statistikk.antall shouldBe 1
        statistikk.antallPerVedtaksaar shouldBe mapOf("2025" to 1L)
    }

    @Test
    fun `ingen treff gir tomt resultat`() {
        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null)

        statistikk.antall shouldBe 0
        statistikk.antallPerVedtaksaar.shouldBeEmpty()
        statistikk.saker!!.shouldBeEmpty()
    }

    /** Persisterer en EU/EØS-sak med én behandling, behandlingsresultat, evt. vedtaksdato og en anmodningsprosess. */
    private fun lagSak(
        saksnummer: String,
        erFjernarbeid: Boolean,
        resultattype: Behandlingsresultattyper,
        vedtaksdato: Instant?,
    ): Long = lagBehandling(opprettFagsak(saksnummer), erFjernarbeid, resultattype, vedtaksdato)

    private fun opprettFagsak(saksnummer: String): Fagsak = fagsakRepository.save(
        Fagsak.forTest {
            this.saksnummer = saksnummer
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            status = Saksstatuser.LOVVALG_AVKLART
            medBruker()
        },
    )

    /** Legger en behandling med behandlingsresultat og anmodningsprosess på en allerede lagret fagsak. */
    private fun lagBehandling(
        fagsak: Fagsak,
        erFjernarbeid: Boolean,
        resultattype: Behandlingsresultattyper,
        vedtaksdato: Instant?,
    ): Long {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = resultattype
            fastsattAvLand = Land_iso2.NO
            vedtakMetadata {
                vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
                this.vedtaksdato = vedtaksdato
            }
            behandling {
                medFagsak(fagsak)
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                tema = Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
            }
        }

        // Fagsaken er allerede lagret og lagres ikke på nytt, så behandlingen persisteres kun via
        // behandlingsresultatet. Fagsak.behandlinger er inverssiden uten orphanRemoval og trenger ingen opprydding
        val lagret = behandlingsresultatRepository.saveAndFlush(behandlingsresultat)

        val behandlingId = lagret.hentBehandling().id
        lagProsess(behandlingId, erFjernarbeid)
        return behandlingId
    }

    private fun lagProsess(
        behandlingId: Long,
        erFjernarbeid: Boolean,
        prosessType: ProsessType = ProsessType.ANMODNING_OM_UNNTAK,
    ) {
        val behandling = behandlingsresultatRepository.findById(behandlingId).orElseThrow().hentBehandling()
        val prosessinstans = Prosessinstans.forTest {
            id = null
            type = prosessType
            medBehandling(behandling)
            medData(ProsessDataKey.ER_FJERNARBEID_TWFA, erFjernarbeid)
        }
        prosessinstansRepository.saveAndFlush(prosessinstans)
    }

    private fun dato(år: Int, måned: Int, dag: Int): Instant =
        LocalDate.of(år, måned, dag).atStartOfDay(ZoneId.systemDefault()).toInstant()
}
