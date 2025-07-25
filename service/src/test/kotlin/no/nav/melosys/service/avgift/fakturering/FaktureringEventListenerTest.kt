package no.nav.melosys.service.avgift.fakturering

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.AVSLUTTET
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.VURDER_DOKUMENT
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.aktoer.AktoerHistorikkService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@ExtendWith(MockKExtension::class)
internal class FaktureringEventListenerTest {
    @MockK
    private lateinit var behandlingService: BehandlingService
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService
    @MockK
    private lateinit var aktoerHistorikkService: AktoerHistorikkService
    @MockK
    private lateinit var trygdeavgiftService: TrygdeavgiftService
    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    private lateinit var faktureringEventListener: FaktureringEventListener

    @BeforeEach
    fun setup() {
        faktureringEventListener = FaktureringEventListener(
            behandlingService,
            behandlingsresultatService,
            aktoerHistorikkService,
            trygdeavgiftService,
            prosessinstansService
        )


    }

    @Test
    fun oppdaterFakturaMottakerHvisNødvendig() {
        val nåværendeFullmektigAvgift = Aktoer().apply {
            id = 1
            registrertDato =  LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            endretDato = registrertDato
            orgnr = "888888888"
            rolle = Aktoersroller.FULLMEKTIG
        }
        val fullmakt = Fullmakt().apply {
            aktoer = nåværendeFullmektigAvgift
            type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT
        }
        nåværendeFullmektigAvgift.fullmakter = setOf(fullmakt)
        val fagsak = FagsakTestFactory.builder().aktører(nåværendeFullmektigAvgift).build()
        val avsluttetBehandling = Behandling.buildForTest {
            id = 1
            status = AVSLUTTET
            registrertDato = Instant.EPOCH
            this.fagsak = fagsak
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = avsluttetBehandling
            vedtakMetadata = null
        }

        val historiskFullmektig = Aktoer().apply {
            id = 1
            registrertDato = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            endretDato = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            orgnr = "999999999"
            rolle = Aktoersroller.FULLMEKTIG
        }
        val historiskFullmakt = Fullmakt().apply {
            aktoer = historiskFullmektig
            type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT
        }
        historiskFullmektig.apply {
            fullmakter = setOf(historiskFullmakt)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(avsluttetBehandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(avsluttetBehandling.id) } returns avsluttetBehandling
        every {
            aktoerHistorikkService.hentHistoriskeAktørerPåTidspunkt(
                fagsak,
                Aktoersroller.FULLMEKTIG,
                avsluttetBehandling.registrertDato
            )
        } returns listOf(historiskFullmektig)
        every { trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(fagsak.saksnummer, true) } returns true
        every { prosessinstansService.opprettProsessinstansOppdaterFaktura(avsluttetBehandling) } just runs


        faktureringEventListener.oppdaterFakturaMottakerHvisNødvendig(
            BehandlingEndretStatusEvent(
                AVSLUTTET,
                avsluttetBehandling
            )
        )


        verify { prosessinstansService.opprettProsessinstansOppdaterFaktura(avsluttetBehandling) }
    }

    @Test
    fun `Ikke oppdater fakturamottaker hvis behandling ikke er avsluttet`() {
        val behandling = Behandling.buildForTest {
            id = 7
        }

        faktureringEventListener.oppdaterFakturaMottakerHvisNødvendig(
            BehandlingEndretStatusEvent(
                VURDER_DOKUMENT,
                behandling
            )
        )

        verify { prosessinstansService wasNot called }
    }

    @Test
    fun `Ikke oppdater fakturamottaker hvis en behandling avsluttes med vedtak, siden fakturamottaker oppdateres ifm vedtak allerede`() {
        val nåværendeFullmektigAvgift = Aktoer().apply {
            id = 1
            registrertDato =  LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            endretDato = registrertDato
            orgnr = "888888888"
            rolle = Aktoersroller.FULLMEKTIG
        }
        val fullmakt = Fullmakt().apply {
            aktoer = nåværendeFullmektigAvgift
            type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT
        }
        nåværendeFullmektigAvgift.fullmakter = setOf(fullmakt)
        val fagsak = FagsakTestFactory.builder().aktører(nåværendeFullmektigAvgift).build()
        val avsluttetBehandling = Behandling.buildForTest {
            id = 1
            status = AVSLUTTET
            registrertDato = Instant.EPOCH
            this.fagsak = fagsak
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = avsluttetBehandling
            vedtakMetadata = VedtakMetadata().apply {
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
        }

        val historiskFullmektig = Aktoer().apply {
            id = 1
            registrertDato = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            endretDato = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            orgnr = "999999999"
            rolle = Aktoersroller.FULLMEKTIG
        }
        val historiskFullmakt = Fullmakt().apply {
            aktoer = historiskFullmektig
            type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT
        }
        historiskFullmektig.apply {
            fullmakter = setOf(historiskFullmakt)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(avsluttetBehandling.id) } returns behandlingsresultat


        faktureringEventListener.oppdaterFakturaMottakerHvisNødvendig(
            BehandlingEndretStatusEvent(
                AVSLUTTET,
                avsluttetBehandling
            )
        )


        verify { prosessinstansService wasNot called }
    }

    @Test
    fun `Hvis tidligere fullmektig ble fjernet, skal bruker få fakturaer`() {
        val fagsak = FagsakTestFactory.lagFagsak()
        val avsluttetBehandling = Behandling.buildForTest {
            id = 1
            status = AVSLUTTET
            registrertDato = Instant.EPOCH
            this.fagsak = fagsak
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = avsluttetBehandling
            vedtakMetadata = null
        }

        val historiskFullmektig = Aktoer().apply {
            id = 1
            registrertDato = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            endretDato = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
            orgnr = "999999999"
            rolle = Aktoersroller.FULLMEKTIG
        }
        val historiskFullmakt = Fullmakt().apply {
            aktoer = historiskFullmektig
            type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT
        }
        historiskFullmektig.apply {
            fullmakter = setOf(historiskFullmakt)
        }

        every { behandlingsresultatService.hentBehandlingsresultat(avsluttetBehandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(avsluttetBehandling.id) } returns avsluttetBehandling
        every {
            aktoerHistorikkService.hentHistoriskeAktørerPåTidspunkt(
                fagsak,
                Aktoersroller.FULLMEKTIG,
                avsluttetBehandling.registrertDato
            )
        } returns listOf(historiskFullmektig)
        every { trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(fagsak.saksnummer, true) } returns true
        every { prosessinstansService.opprettProsessinstansOppdaterFaktura(avsluttetBehandling) } just runs


        faktureringEventListener.oppdaterFakturaMottakerHvisNødvendig(
            BehandlingEndretStatusEvent(
                AVSLUTTET,
                avsluttetBehandling
            )
        )


        verify { prosessinstansService.opprettProsessinstansOppdaterFaktura(avsluttetBehandling) }
    }
}
