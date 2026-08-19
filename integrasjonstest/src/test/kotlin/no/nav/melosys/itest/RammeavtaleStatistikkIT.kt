package no.nav.melosys.itest

import io.kotest.assertions.withClue
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
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
 * ferdigbehandlede saker (fastsatt lovvalg med vedtaksdato), og at året som telles er vedtaksåret.
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
    }

    @Test
    fun `ingen treff gir tomt resultat`() {
        val statistikk = rammeavtaleStatistikkService.hentRammeavtaleFjernarbeidStatistikk(null, null)

        statistikk.antall shouldBe 0
        statistikk.antallPerVedtaksaar.shouldBeEmpty()
    }

    /** Persisterer en EU/EØS-sak med behandlingsresultat, evt. vedtaksdato og en anmodning-om-unntak-prosess. */
    private fun lagSak(
        saksnummer: String,
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
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.AVSLUTTET
                tema = Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
                fagsak {
                    this.saksnummer = saksnummer
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                    status = Saksstatuser.LOVVALG_AVKLART
                    medBruker()
                }
            }
        }

        val fagsak = behandlingsresultat.hentBehandling().fagsak
        // Rydder den toveis relasjonen slik at Fagsak og Behandlingsresultat kan lagres hver for seg
        fagsak.behandlinger.clear()
        fagsakRepository.save(fagsak)
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
