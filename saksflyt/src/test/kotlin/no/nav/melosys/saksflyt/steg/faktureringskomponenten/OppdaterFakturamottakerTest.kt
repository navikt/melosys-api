package no.nav.melosys.saksflyt.steg.faktureringskomponenten

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.SAKSNUMMER
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaMottakerDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FullmektigDto
import no.nav.melosys.saksflyt.steg.fakturering.OppdaterFakturamottaker
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.prosessinstansForTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockKExtension::class)
class OppdaterFakturamottakerTest {

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer

    private val SAKSBEHANDLER_IDENT = "S123456"
    private val BEHANDLING_ID = 1L

    private lateinit var oppdaterFakturamottaker: OppdaterFakturamottaker

    @BeforeEach
    fun setup() {
        oppdaterFakturamottaker = OppdaterFakturamottaker(fagsakService, behandlingsresultatService, faktureringskomponentenConsumer)
    }

    @Test
    fun utfør_ingenBehandlingerMedFakturaserieReferanser_kallerIkkeFaktureringskomponenten() {
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns Fagsak.forTest {
            behandling { id = BEHANDLING_ID }
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns Behandlingsresultat()


        oppdaterFakturamottaker.utfør(prosessinstansForTest { medData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER) })

        verify { fagsakService.hentFagsak(SAKSNUMMER) }
        verify { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }
        verify { faktureringskomponentenConsumer wasNot Called }
    }

    @Test
    fun utfør_flereBetalingerMedReferanse_kallerFaktureringskomponentMedNyligsteReferanse() {
        val fagsak = Fagsak.forTest {
            medBruker {
                rolle = Aktoersroller.FULLMEKTIG
                setFullmaktstype(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
            }
            behandling {
                id = BEHANDLING_ID
                registrertDato = Instant.now().minus(31, ChronoUnit.DAYS)
            }
            behandling {
                id = 2L
                registrertDato = Instant.now()
            }
        }
        val behandlingsresultat1 = Behandlingsresultat().apply { fakturaserieReferanse = "1" }
        val behandlingsresultat2 = Behandlingsresultat().apply { fakturaserieReferanse = "2" }
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat1
        every { behandlingsresultatService.hentBehandlingsresultat(2L) } returns behandlingsresultat2

        val prosessinstans = prosessinstansForTest {
            medData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER_IDENT)
            medData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER)
        }


        oppdaterFakturamottaker.utfør(prosessinstans)


        verify { fagsakService.hentFagsak(SAKSNUMMER) }
        verify { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }
        verify {
            faktureringskomponentenConsumer.oppdaterFakturaMottaker(
                behandlingsresultat2.fakturaserieReferanse,
                FakturaMottakerDto(FullmektigDto(Aktoer().apply {
                    this.rolle = Aktoersroller.FULLMEKTIG
                    setFullmaktstype(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
                })),
                eq(SAKSBEHANDLER_IDENT)
            )
        }
    }

    @Test
    fun utfør_referanseMenIngenFullmektig_kallerFaktureringskomponentMedTomFullmektig() {
        val fagsak = Fagsak.forTest { behandlinger(Behandling.forTest { id = BEHANDLING_ID }) }
        val behandlingsresultat = Behandlingsresultat().apply { fakturaserieReferanse = "1" }
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        val prosessinstans = prosessinstansForTest {
            medData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER_IDENT)
            medData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER)
        }


        oppdaterFakturamottaker.utfør(prosessinstans)


        verify { fagsakService.hentFagsak(SAKSNUMMER) }
        verify { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }
        verify {
            faktureringskomponentenConsumer.oppdaterFakturaMottaker(
                behandlingsresultat.fakturaserieReferanse,
                FakturaMottakerDto(FullmektigDto(null)),
                eq(SAKSBEHANDLER_IDENT)
            )
        }
    }
}
