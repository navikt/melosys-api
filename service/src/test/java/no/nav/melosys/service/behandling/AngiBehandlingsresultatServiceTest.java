package no.nav.melosys.service.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
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
    private FagsakService fagsakService;

    private AngiBehandlingsresultatService angiBehandlingsresultatService;

    @BeforeEach
    public void setup() {
        angiBehandlingsresultatService = new AngiBehandlingsresultatService(behandlingsresultatService, fagsakService);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_gyldigScenario_kallerKorrekt() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.MEDLEMSKAP_LOVVALG, Sakstyper.FTRL, Behandlingstyper.FØRSTEGANG, Behandlingstema.YRKESAKTIV);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        angiBehandlingsresultatService
            .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);


        verify(fagsakService).avsluttFagsakOgBehandling(behandlingsresultat.getBehandling().getFagsak(), Saksstatuser.LOVVALG_AVKLART);
        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultatArgumentCaptor.getValue().getType()).isEqualTo(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);
    }

    @Test
    void oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling_ugyldigScenario_kasterFeilmelding() {
        var behandlingsresultat = lagBehandlingsresultat(Sakstemaer.UNNTAK, Sakstyper.EU_EOS, Behandlingstyper.HENVENDELSE, Behandlingstema.ARBEID_ETT_LAND_ØVRIG);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> angiBehandlingsresultatService
                .oppdaterBehandlingsresultattypeOgAvsluttFagsakOgBehandling(BEHANDLING_ID, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN))
            .withMessageContaining("Denne saken kan ikke sette behandlingsresultattype til MEDLEM_I_FOLKETRYGDEN");
    }

    private Behandlingsresultat lagBehandlingsresultat(Sakstemaer sakstema, Sakstyper sakstype, Behandlingstyper behandlingstype, Behandlingstema behandlingstema) {
        var fagsak = new Fagsak();
        fagsak.setTema(sakstema);
        fagsak.setType(sakstype);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setType(behandlingstype);
        behandling.setTema(behandlingstema);
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        return behandlingsresultat;
    }
}
