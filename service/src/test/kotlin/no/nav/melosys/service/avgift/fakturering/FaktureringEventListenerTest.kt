package no.nav.melosys.service.avgift.fakturering

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.AVSLUTTET
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.aktoer.AktoerHistorikkService
import no.nav.melosys.service.behandling.BehandlingService
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
    private lateinit var aktoerHistorikkService: AktoerHistorikkService
    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    private lateinit var faktureringEventListener: FaktureringEventListener

    @BeforeEach
    fun setup() {
        faktureringEventListener = FaktureringEventListener(behandlingService, aktoerHistorikkService, prosessinstansService)
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
        val fagsak = Fagsak().apply {
            saksnummer = "MEL-test"
            aktører = setOf(nåværendeFullmektigAvgift)
        }
        val avsluttetBehandling = Behandling().apply {
            id = 1
            status = AVSLUTTET
            registrertDato = Instant.EPOCH
            this.fagsak = fagsak
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

        every { behandlingService.hentBehandling(avsluttetBehandling.id) } returns avsluttetBehandling
        every {
            aktoerHistorikkService.hentGyldigeAktørerPåTidspunkt(
                fagsak,
                Aktoersroller.FULLMEKTIG,
                avsluttetBehandling.registrertDato
            )
        } returns listOf(historiskFullmektig)
        every { prosessinstansService.opprettProsessinstansOppdaterFaktura(fagsak.saksnummer) } just runs


        faktureringEventListener.oppdaterFakturaMottakerHvisNødvendig(
            BehandlingEndretStatusEvent(
                AVSLUTTET,
                avsluttetBehandling
            )
        )


        verify { prosessinstansService.opprettProsessinstansOppdaterFaktura(fagsak.saksnummer) }
    }
}
