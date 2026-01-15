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

    private fun lagFullmektigMedTrygdeavgiftFullmakt(
        orgnr: String,
        registrertDato: Instant,
        endretDato: Instant = registrertDato
    ): Aktoer {
        val aktoer = Aktoer().apply {
            id = 1
            this.registrertDato = registrertDato
            this.endretDato = endretDato
            this.orgnr = orgnr
            rolle = Aktoersroller.FULLMEKTIG
        }
        val fullmakt = Fullmakt().apply {
            this.aktoer = aktoer
            type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT
        }
        aktoer.fullmakter = setOf(fullmakt)
        return aktoer
    }

    @Test
    fun oppdaterFakturaMottakerHvisNødvendig() {
        val desember1 = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
        val desember2 = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        val nåværendeFullmektigAvgift = lagFullmektigMedTrygdeavgiftFullmakt(
            orgnr = "888888888",
            registrertDato = desember1
        )
        val fagsak = Fagsak.forTest { aktører(nåværendeFullmektigAvgift) }
        val avsluttetBehandling = Behandling.forTest {
            id = 1
            status = AVSLUTTET
            registrertDato = Instant.EPOCH
            this.fagsak = fagsak
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling = avsluttetBehandling
        }

        val historiskFullmektig = lagFullmektigMedTrygdeavgiftFullmakt(
            orgnr = "999999999",
            registrertDato = desember1,
            endretDato = desember2
        )

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
        val behandling = Behandling.forTest {
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
        val desember1 = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        val nåværendeFullmektigAvgift = lagFullmektigMedTrygdeavgiftFullmakt(
            orgnr = "888888888",
            registrertDato = desember1
        )
        val fagsak = Fagsak.forTest { aktører(nåværendeFullmektigAvgift) }
        val avsluttetBehandling = Behandling.forTest {
            id = 1
            status = AVSLUTTET
            registrertDato = Instant.EPOCH
            this.fagsak = fagsak
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling = avsluttetBehandling
            vedtakMetadata {
                vedtakstype = Vedtakstyper.ENDRINGSVEDTAK
            }
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
        val desember1 = LocalDate.of(2023, 12, 1).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()
        val desember2 = LocalDate.of(2023, 12, 2).atStartOfDay(ZoneId.of("Europe/Oslo")).toInstant()

        val fagsak = Fagsak.forTest()
        val avsluttetBehandling = Behandling.forTest {
            id = 1
            status = AVSLUTTET
            registrertDato = Instant.EPOCH
            this.fagsak = fagsak
        }
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling = avsluttetBehandling
        }

        val historiskFullmektig = lagFullmektigMedTrygdeavgiftFullmakt(
            orgnr = "999999999",
            registrertDato = desember1,
            endretDato = desember2
        )

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
