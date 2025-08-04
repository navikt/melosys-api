package no.nav.melosys.saksflyt.steg.jfr

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.sak.ArkivsakService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class OpprettArkivsakKtTest {

    @Mock
    private lateinit var arkivsakService: ArkivsakService

    @Mock
    private lateinit var fagsakService: FagsakService

    private lateinit var opprettArkivsak: OpprettArkivsak

    private val oppgaveFactory = no.nav.melosys.service.oppgave.OppgaveFactory()

    @BeforeEach
    fun setUp() {
        opprettArkivsak = OpprettArkivsak(fagsakService, arkivsakService, oppgaveFactory)
    }

    @Test
    fun utfør_arkivsakIDEksistererIkkeFraFør_arkivsakBlirOpprettet() {
        val forventetArkivsakID = 1234432L

        val fagsak = FagsakTestFactory.builder().medBruker().build()

        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medFagsak(fagsak)
            .build()

        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling

        `when`(
            arkivsakService.opprettSakForBruker(
                fagsak.saksnummer,
                oppgaveFactory.utledTema(fagsak.type, fagsak.tema, behandling.tema, behandling.type),
                FagsakTestFactory.BRUKER_AKTØR_ID
            )
        ).thenReturn(forventetArkivsakID)
        opprettArkivsak.utfør(prosessinstans)

        fagsak.gsakSaksnummer shouldBe forventetArkivsakID
        verify(fagsakService).lagre(fagsak)
    }

    @Test
    fun utfør_arkivsakIDEksistererIkkeFraFør_arkivsakBlirOpprettet_brukFagsakTema() {
        val forventetArkivsakID = 1234432L

        val fagsak = FagsakTestFactory.builder().medBruker().build()

        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.YRKESAKTIV)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medFagsak(fagsak)
            .build()

        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling

        `when`(
            arkivsakService.opprettSakForBruker(
                fagsak.saksnummer, oppgaveFactory.utledTema(fagsak.type, fagsak.tema, behandling.tema, behandling.type),
                FagsakTestFactory.BRUKER_AKTØR_ID
            )
        ).thenReturn(forventetArkivsakID)
        opprettArkivsak.utfør(prosessinstans)

        fagsak.gsakSaksnummer shouldBe forventetArkivsakID
        verify(fagsakService).lagre(fagsak)
    }

    @Test
    fun utfør_virksomhetErHovedpart_oppretterSakForVirksomhet() {
        val forventetArkivsakID = 1234432L

        val fagsak = FagsakTestFactory.builder().medVirksomhet().build()

        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.YRKESAKTIV)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medFagsak(fagsak)
            .build()

        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling

        `when`(
            arkivsakService.opprettSakForVirksomhet(
                fagsak.saksnummer,
                oppgaveFactory.utledTema(fagsak.type, fagsak.tema, behandling.tema, behandling.type),
                FagsakTestFactory.ORGNR
            )
        ).thenReturn(forventetArkivsakID)
        opprettArkivsak.utfør(prosessinstans)

        fagsak.gsakSaksnummer shouldBe forventetArkivsakID
        verify(fagsakService).lagre(fagsak)
    }

    @Test
    fun utfør_arkivsakIDEksisterer_kasterException() {
        val fagsak = FagsakTestFactory.builder().medBruker().build()
        fagsak.gsakSaksnummer = 1234432L

        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.YRKESAKTIV)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medFagsak(fagsak)
            .build()

        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling

        val exception = shouldThrow<FunksjonellException> {
            opprettArkivsak.utfør(prosessinstans)
        }
        exception.message shouldContain "allerede knyttet til"
    }

    @Test
    fun utfør_harVerkenBrukerIDEllerVirksomhetOrgnr_kasterException() {
        val fagsak = FagsakTestFactory.builder().build()

        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.YRKESAKTIV)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medFagsak(fagsak)
            .build()

        val prosessinstans = Prosessinstans()
        prosessinstans.behandling = behandling

        val exception = shouldThrow<FunksjonellException> {
            opprettArkivsak.utfør(prosessinstans)
        }
        exception.message shouldContain "Finner verken bruker eller virksomhet tilknyttet fagsak MEL-test"
    }
}
