package no.nav.melosys.saksflyt.steg.behandling

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.repository.AktoerRepository
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.aktoer.AktoerService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvklarArbeidsgiverTest {

    @MockK
    private lateinit var aktoerService: AktoerService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @MockK
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    private lateinit var avklarArbeidsgiver: AvklarArbeidsgiver
    private lateinit var prosessinstans: Prosessinstans
    private lateinit var fagsak: Fagsak
    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var lovvalgsperiode: Lovvalgsperiode

    private var avklartVirksomhet = AvklartVirksomhet("Test", "123456789", null, Yrkesaktivitetstyper.LOENNET_ARBEID)

    @BeforeEach
    fun setUp() {
        every { aktoerService.erstattEksisterendeArbeidsgiveraktører(any(), any()) } just Runs
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { avklarteVirksomheterService.hentNorskeArbeidsgivere(any(), any()) } returns emptyList()
        avklarArbeidsgiver =
            AvklarArbeidsgiver(aktoerService, avklarteVirksomheterService, behandlingService, behandlingsresultatService, saksbehandlingRegler)

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.IVERKSETT_VEDTAK_EOS
            status = ProsessStatus.KLAR
            behandling {
                id = 1L
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
                fagsak {
                    medBruker()
                }
            }
        }
        fagsak = prosessinstans.hentBehandling.fagsak

        every { behandlingService.hentBehandlingMedSaksopplysninger(any()) } returns prosessinstans.behandling

        lovvalgsperiode = Lovvalgsperiode().apply {
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }

        behandlingsresultat = Behandlingsresultat().apply {
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            lovvalgsperioder = setOf(lovvalgsperiode)
        }


        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
    }

    @Test
    fun `utfør med avklart norsk virksomhet arbeidsgiveraktør opprettes`() {
        val aktoerRepository = mockk<AktoerRepository>()
        val steg = AvklarArbeidsgiver(
            AktoerService(aktoerRepository),
            avklarteVirksomheterService,
            behandlingService,
            behandlingsresultatService,
            saksbehandlingRegler
        )

        val avklarteVirksomheter = listOf(avklartVirksomhet)
        every { avklarteVirksomheterService.hentNorskeArbeidsgivere(any(), any()) } returns avklarteVirksomheter
        every { aktoerRepository.deleteAllByFagsakAndRolle(any(), any()) } just Runs
        every { aktoerRepository.save(any<Aktoer>()) } returnsArgument 0
        every { aktoerRepository.flush() } just Runs


        steg.utfør(prosessinstans)


        verify { aktoerRepository.deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER) }

        verify { aktoerRepository.save(any<Aktoer>()) }
    }

    @Test
    fun `utfør med ingen flyt arbeidsgiver avklares ikke`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true
        prosessinstans.hentBehandling.type = Behandlingstyper.HENVENDELSE


        avklarArbeidsgiver.utfør(prosessinstans)


        verify(exactly = 0) { aktoerService.erstattEksisterendeArbeidsgiveraktører(any(), any()) }
    }

    @Test
    fun `utfør uten avklart norsk virksomhet arbeidsgiveraktører slettes`() {
        val aktoerRepository = mockk<AktoerRepository>()
        val steg = AvklarArbeidsgiver(
            AktoerService(aktoerRepository),
            avklarteVirksomheterService,
            behandlingService,
            behandlingsresultatService,
            saksbehandlingRegler
        )

        every { aktoerRepository.deleteAllByFagsakAndRolle(any(), any()) } just Runs
        every { aktoerRepository.flush() } just Runs


        steg.utfør(prosessinstans)


        verify { aktoerRepository.deleteAllByFagsakAndRolle(fagsak, Aktoersroller.ARBEIDSGIVER) }
        verify(exactly = 0) { aktoerRepository.save(any<Aktoer>()) }
    }

    @Test
    fun `utfør iverksett vedtak art12 arbeidsgiver aktører skal opprettes`() {
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1


        avklarArbeidsgiver.utfør(prosessinstans)


        verify { aktoerService.erstattEksisterendeArbeidsgiveraktører(any(), any()) }
    }

    @Test
    fun `utfør iverksett vedtak art13 arbeidsgiver aktører skal ikke opprettes`() {
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A


        avklarArbeidsgiver.utfør(prosessinstans)


        verify(exactly = 0) { aktoerService.erstattEksisterendeArbeidsgiveraktører(any(), any()) }
    }

    @Test
    fun `utfør trygdeavtale samt ikke avslag manglende opplysning arbeidsgiver aktør opprettes`() {
        fagsak.type = Sakstyper.TRYGDEAVTALE
        prosessinstans.hentBehandling.tema = Behandlingstema.YRKESAKTIV
        behandlingsresultat.lovvalgsperioder = mutableSetOf()


        avklarArbeidsgiver.utfør(prosessinstans)


        verify { aktoerService.erstattEksisterendeArbeidsgiveraktører(any(), any()) }
    }
}
