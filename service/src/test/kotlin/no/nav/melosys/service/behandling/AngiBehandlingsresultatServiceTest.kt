package no.nav.melosys.service.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
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
class AngiBehandlingsresultatServiceTest {

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var oppgaveService: OppgaveService

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    private lateinit var angiBehandlingsresultatService: AngiBehandlingsresultatService

    @BeforeEach
    fun setup() {
        angiBehandlingsresultatService = AngiBehandlingsresultatService(behandlingsresultatService, oppgaveService, fagsakService)
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario MEDLEM_I_FOLKETRYGDEN`() {
        val behandlingsresultat =
            lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.FTRL, Behandlingstyper.FØRSTEGANG, Behandlingstema.YRKESAKTIV)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario UNNTATT_MEDLEMSKAP`() {
        val behandlingsresultat =
            lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.FTRL, Behandlingstyper.FØRSTEGANG, Behandlingstema.UNNTAK_MEDLEMSKAP)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.UNNTATT_MEDLEMSKAP)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.UNNTATT_MEDLEMSKAP
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario REGISTRERT_UNNTAK`() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.REGISTRERT_UNNTAK)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.REGISTRERT_UNNTAK
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario DELVIS_GODKJENT_UNNTAK`() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.DELVIS_GODKJENT_UNNTAK)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.DELVIS_GODKJENT_UNNTAK
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kaste feilmelding for ugyldig scenario DELVIS_GODKJENT_UNNTAK`() {
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
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario MEDLEM_I_FOLKETRYGDEN utvidet`() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario FASTSATT_LOVVALGSLAND`() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.TRYGDEAVTALE,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ARBEID_KUN_NORGE
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario AVSLAG_SØKNAD`() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.EU_EOS,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.AVSLAG_SØKNAD)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.AVSLAG_SØKNAD
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario KLAGE`() {
        val behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.EU_EOS, Behandlingstyper.KLAGE)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.KLAGEINNSTILLING)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.KLAGEINNSTILLING
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario NY_VURDERING`() {
        val behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.EU_EOS, Behandlingstyper.NY_VURDERING)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.OMGJORT)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.OMGJORT
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kalle korrekt for gyldig scenario A1_ANMODNING_UNNTAK_PAPIR`() {
        val behandlingsresultat =
            lagBehandlingsresultat(Sakstemaer.UNNTAK, Sakstyper.EU_EOS, Behandlingstyper.FØRSTEGANG, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.REGISTRERT_UNNTAK)


        verify { fagsakService.avsluttFagsakOgBehandling(behandlingsresultat.hentBehandling().fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify { oppgaveService.ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID) }
        val captor = slot<Behandlingsresultat>()
        verify { behandlingsresultatService.lagre(capture(captor)) }
        captor.captured.type shouldBe Behandlingsresultattyper.REGISTRERT_UNNTAK
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal kaste feilmelding for ugyldig scenario`() {
        val behandlingsresultat =
            lagBehandlingsresultat(Sakstemaer.UNNTAK, Sakstyper.EU_EOS, Behandlingstyper.HENVENDELSE, Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY)
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        shouldThrow<FunksjonellException> {
            angiBehandlingsresultatService
                .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN)
        }.message shouldContain "Kan ikke endre behandlingsresultattype"
    }

    @Test
    fun `oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling skal fjerne medlemskapsperioder når FTRL og gyldig resultattype`() {
        val behandlingsresultat = lagBehandlingsresultat(
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Sakstyper.FTRL,
            Behandlingstyper.FØRSTEGANG,
            Behandlingstema.YRKESAKTIV
        ).apply {
            id = 1L
            medlemskapsperioder = mutableSetOf()
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


        verify { behandlingsresultatService.tømMedlemskapsperioder(behandlingsresultat.hentId()) }
    }

    private fun lagBehandlingsresultat(sakstema: Sakstemaer, sakstype: Sakstyper, behandlingstype: Behandlingstyper): Behandlingsresultat =
        lagBehandlingsresultat(
            sakstema, sakstype, behandlingstype,
            Behandlingstema.YRKESAKTIV // Tema spiller ingen rolle for testen, men kan ikke lengre være null
        )

    private fun lagBehandlingsresultat(
        sakstema: Sakstemaer,
        sakstype: Sakstyper,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema
    ): Behandlingsresultat = Behandlingsresultat().apply {
        this.behandling = Behandling.forTest {
            id = BEHANDLING_ID
            type = behandlingstype
            tema = behandlingstema
            fagsak {
                tema = sakstema
                type = sakstype
            }
        }
    }

    companion object {
        private const val BEHANDLING_ID = 1L
    }
}
