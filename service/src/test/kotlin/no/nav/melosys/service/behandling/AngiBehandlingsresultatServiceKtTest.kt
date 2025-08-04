package no.nav.melosys.service.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class AngiBehandlingsresultatServiceKtTest {

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    private lateinit var angiBehandlingsresultatService: AngiBehandlingsresultatService

    companion object {
        private const val BEHANDLING_ID = 1L
    }

    @BeforeEach
    fun setup() {
        angiBehandlingsresultatService = AngiBehandlingsresultatService(behandlingsresultatService, oppgaveService, fagsakService)
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioMEDLEM_I_FOLKETRYGDEN_kallerKorrekt() {
        val behandlingsresultat =
            lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.FTRL, Behandlingstyper.FØRSTEGANG, Behandlingstema.YRKESAKTIV)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioUNNTATT_MEDLEMSKAP_kallerKorrekt() {
        val behandlingsresultat =
            lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.FTRL, Behandlingstyper.FØRSTEGANG, Behandlingstema.UNNTAK_MEDLEMSKAP)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.UNNTATT_MEDLEMSKAP)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.UNNTATT_MEDLEMSKAP
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioREGISTRERT_UNNTAK_kallerKorrekt() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.REGISTRERT_UNNTAK)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.REGISTRERT_UNNTAK
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioDELVIS_GODKJENT_UNNTAK_kallerKorrekt() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.DELVIS_GODKJENT_UNNTAK)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.DELVIS_GODKJENT_UNNTAK
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_ugyldigScenario_DELVIS_GODKJENT_UNNTAK_kasterFeilmelding() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ARBEID_KUN_NORGE
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        shouldThrow<FunksjonellException> {
            angiBehandlingsresultatService
                .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.DELVIS_GODKJENT_UNNTAK)
        }.message shouldContain "Kan ikke endre behandlingsresultattype"
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioMEDLEM_I_FOLKETRYGDEN_utvidet_kallerKorrekt() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioFASTSATT_LOVVALGSLAND_kallerKorrekt() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ARBEID_KUN_NORGE
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioAVSLAG_SØKNAD_kallerKorrekt() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.EU_EOS,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.AVSLAG_SØKNAD)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.AVSLAG_SØKNAD
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioKLAGE_kallerKorrekt() {
        val behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.EU_EOS, Behandlingstyper.KLAGE)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.KLAGEINNSTILLING)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.KLAGEINNSTILLING
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioNY_VURDERING_kallerKorrekt() {
        val behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.EU_EOS, Behandlingstyper.NY_VURDERING)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.OMGJORT)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.OMGJORT
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioA1_ANMODNING_UNNTAK_PAPIR_kasterKorrekt() {
        val behandlingsresultat =
            lagBehandlingsresultat(Sakstemaer.UNNTAK, Sakstyper.EU_EOS, Behandlingstyper.FØRSTEGANG, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.REGISTRERT_UNNTAK)

        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.REGISTRERT_UNNTAK
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_ugyldigScenario_kasterFeilmelding() {
        val behandlingsresultat =
            lagBehandlingsresultat(Sakstemaer.UNNTAK, Sakstyper.EU_EOS, Behandlingstyper.HENVENDELSE, Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        shouldThrow<FunksjonellException> {
            angiBehandlingsresultatService
                .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
        }.message shouldContain "Kan ikke endre behandlingsresultattype"
    }

    @Test
    fun oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_fjernerMedlemskapsperioderNårFTRLOgGyldigResultattype() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.FTRL,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV
        ).apply {
            id = 1L
            medlemskapsperioder = ArrayList()
        }

        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2020, 1, 1)
            tom = LocalDate.of(2021, 1, 1)
        }

        behandlingsresultat.medlemskapsperioder.add(medlemskapsperiode)
        behandlingsresultat.type = Behandlingsresultattyper.AVSLAG_SØKNAD

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat

        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(
                BEHANDLING_ID,
                Behandlingsresultattyper.AVSLAG_SØKNAD
            )

        verify { behandlingsresultatService.tømMedlemskapsperioder(behandlingsresultat.id) }
    }

    private fun lagBehandlingsresultat(sakstema: Sakstemaer, sakstype: Sakstyper, behandlingstype: Behandlingstyper): Behandlingsresultat {
        return lagBehandlingsresultat(
            sakstema, sakstype, behandlingstype,
            Behandlingstema.YRKESAKTIV // Tema spiller ingen rolle for testen, men kan ikke lengre være null
        )
    }

    private fun lagBehandlingsresultat(
        sakstema: Sakstemaer,
        sakstype: Sakstyper,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Behandlingsresultat {
        val fagsak = FagsakTestFactory.builder()
            .tema(sakstema)
            .type(sakstype)
            .build()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(BEHANDLING_ID)
            .medFagsak(fagsak)
            .medType(behandlingstype)
            .medTema(behandlingstema)
            .build()

        return Behandlingsresultat().apply {
            this.behandling = behandling
        }
    }
}
