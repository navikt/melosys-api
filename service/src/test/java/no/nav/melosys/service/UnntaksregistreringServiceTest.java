package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.mottatteopplysninger.AnmodningEllerAttest;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnntaksregistreringServiceTest {
    private final Long BEHANDLING_ID = 111L;

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> captor;

    private UnntaksregistreringService unntaksregistreringService;

    @BeforeEach
    void init() {
        unntaksregistreringService = new UnntaksregistreringService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService);
    }

    @Test
    void registrerUnntakFraMedlemskap_sakstypeTrygdeavtale_lagrerAltKorrekt() {
        var behandling = lagBehandling(Sakstyper.TRYGDEAVTALE, null, Land_iso2.BA);
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.GODKJENT);

        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID);


        verify(prosessinstansService).opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(FagsakTestFactory.SAKSNUMMER);
        verify(behandlingsresultatService).lagre(captor.capture());

        var capturedBehandlingsresultat = captor.getValue();
        assertThat(capturedBehandlingsresultat).isNotNull();
        assertThat(capturedBehandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        assertThat(capturedBehandlingsresultat.getFastsattAvLand()).isEqualTo(Land_iso2.BA);
    }

    @Test
    void registrerUnntakFraMedlemskap_sakstypeEØS_lagrerAltKorrekt() {
        var behandling = lagBehandling(Sakstyper.EU_EOS, Land_iso2.DK, null);
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.GODKJENT);

        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID);


        verify(prosessinstansService).opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(FagsakTestFactory.SAKSNUMMER);
        verify(behandlingsresultatService).lagre(captor.capture());

        var capturedBehandlingsresultat = captor.getValue();
        assertThat(capturedBehandlingsresultat).isNotNull();
        assertThat(capturedBehandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        assertThat(capturedBehandlingsresultat.getFastsattAvLand()).isEqualTo(Land_iso2.DK);
    }

    @Test
    void registrerUnntakFraMedlemskap_utfallRegistreringUnntakIkkeGodkjent_lagrerAltKorrekt() {
        var behandling = lagBehandling(Sakstyper.EU_EOS, null, null);
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.IKKE_GODKJENT);

        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID);


        verify(prosessinstansService).opprettProsessinstansRegistrerUnntakFraMedlemskap(behandling, Saksstatuser.AVSLUTTET);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(FagsakTestFactory.SAKSNUMMER);
        verify(behandlingsresultatService).lagre(captor.capture());

        var capturedBehandlingsresultat = captor.getValue();
        assertThat(capturedBehandlingsresultat).isNotNull();
        assertThat(capturedBehandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.FERDIGBEHANDLET);
    }

    @Test
    void registrerUnntakFraMedlemskap_mottatteOpplysningerDataIkkeAnmodningEllerAttest_kasterFeil() {
        var behandling = lagBehandling(Sakstyper.EU_EOS, null, null);
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(new SøknadNorgeEllerUtenforEØS());
        var behandlingsresultat = new Behandlingsresultat();

        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> unntaksregistreringService.registrerUnntakFraMedlemskap(BEHANDLING_ID))
            .withMessageContaining("Unntaksregistrering er kun tilgjengelig for behandlinger med AnmodningEllerAttest. Det har ikke behandling");
    }

    private Behandling lagBehandling(Sakstyper sakstype, Land_iso2 avsenderland, Land_iso2 lovvalgsland) {
        var fagsak = FagsakTestFactory.builder().type(sakstype).build();

        var anmodningEllerAttest = new AnmodningEllerAttest();
        anmodningEllerAttest.setAvsenderland(avsenderland);
        anmodningEllerAttest.setLovvalgsland(lovvalgsland);

        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(anmodningEllerAttest);
        return behandling;
    }
}
