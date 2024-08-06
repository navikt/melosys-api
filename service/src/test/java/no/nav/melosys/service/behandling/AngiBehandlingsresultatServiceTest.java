package no.nav.melosys.service.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
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
class AngiBehandlingsresultatServiceTest {
    private static final Long BEHANDLING_ID = 1L;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatArgumentCaptor;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private FagsakService fagsakService;

    private AngiBehandlingsresultatService angiBehandlingsresultatService;

    @BeforeEach
    public void setup() {
        angiBehandlingsresultatService = new AngiBehandlingsresultatService(behandlingsresultatService, oppgaveService, fagsakService);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioMEDLEM_I_FOLKETRYGDEN_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.FTRL, Behandlingstyper.FØRSTEGANG, Behandlingstema.YRKESAKTIV);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioUNNTATT_MEDLEMSKAP_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.FTRL, Behandlingstyper.FØRSTEGANG, Behandlingstema.UNNTAK_MEDLEMSKAP);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.UNNTATT_MEDLEMSKAP);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.UNNTATT_MEDLEMSKAP);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioREGISTRERT_UNNTAK_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.TRYGDEAVTALE, Behandlingstyper.FØRSTEGANG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.REGISTRERT_UNNTAK);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.REGISTRERT_UNNTAK);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioDELVIS_GODKJENT_UNNTAK_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.TRYGDEAVTALE, Behandlingstyper.FØRSTEGANG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.DELVIS_GODKJENT_UNNTAK);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.DELVIS_GODKJENT_UNNTAK);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_ugyldigScenario_DELVIS_GODKJENT_UNNTAK_kasterFeilmelding() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.TRYGDEAVTALE, Behandlingstyper.FØRSTEGANG, Behandlingstema.ARBEID_KUN_NORGE);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> angiBehandlingsresultatService
                .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.DELVIS_GODKJENT_UNNTAK))
            .withMessageContaining("Kan ikke endre behandlingsresultattype");
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioMEDLEM_I_FOLKETRYGDEN_utvidet_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.TRYGDEAVTALE, Behandlingstyper.FØRSTEGANG, Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioFASTSATT_LOVVALGSLAND_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.TRYGDEAVTALE, Behandlingstyper.FØRSTEGANG, Behandlingstema.ARBEID_KUN_NORGE);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioAVSLAG_SØKNAD_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.EU_EOS, Behandlingstyper.FØRSTEGANG, Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.AVSLAG_SØKNAD);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.AVSLAG_SØKNAD);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioKLAGE_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.EU_EOS, Behandlingstyper.KLAGE, null);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.KLAGEINNSTILLING);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.KLAGEINNSTILLING);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioNY_VURDERING_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.EU_EOS, Behandlingstyper.NY_VURDERING, null);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.OMGJORT);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.OMGJORT);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenarioA1_ANMODNING_UNNTAK_PAPIR_kasterKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.UNNTAK, Sakstyper.EU_EOS, Behandlingstyper.FØRSTEGANG, Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.REGISTRERT_UNNTAK);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(BEHANDLING_ID);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.REGISTRERT_UNNTAK);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_ugyldigScenario_kasterFeilmelding() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.UNNTAK, Sakstyper.EU_EOS, Behandlingstyper.HENVENDELSE, Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> angiBehandlingsresultatService
                .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN))
            .withMessageContaining("Kan ikke endre behandlingsresultattype");
    }

    private Behandlingsresultat lagBehandlingsresultat(Sakstemaer sakstema, Sakstyper sakstype, Behandlingstyper behandlingstype, Behandlingstema behandlingstema) {
        var fagsak = FagsakTestFactory.builder()
            .tema(sakstema)
            .type(sakstype)
            .build();
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setType(behandlingstype);
        behandling.setTema(behandlingstema);
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        return behandlingsresultat;
    }
}
