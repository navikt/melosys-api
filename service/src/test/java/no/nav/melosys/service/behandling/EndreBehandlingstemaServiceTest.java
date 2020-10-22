package no.nav.melosys.service.behandling;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SED_FORESPØRSEL;
import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SØKNAD;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveService;

@ExtendWith(MockitoExtension.class)
class EndreBehandlingstemaServiceTest {

    private static final long id = 11L;
    private static final String saksbehandler = "Z000000";
    private final Behandling behandling = new Behandling();
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Captor
    private ArgumentCaptor<Behandling> behandlingArgumentCaptor;
    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveOppdateringArgumentCaptor;


    private EndreBehandlingstemaService endreBehandlingstemaService;

    @BeforeEach
    void setUp() throws MelosysException {
        endreBehandlingstemaService = new EndreBehandlingstemaService(behandlingService, behandlingsresultatService, oppgaveService);

        behandling.setId(id);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSøknadBehandlingstema_returnererSøknadBehandlinstema() throws MelosysException {
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);
        when(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler)).thenReturn(true);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id,saksbehandler);
        assertThat(BEHANDLINGSTEMA_SØKNAD).isEqualTo(muligeBehandlingstema);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSEDForespørselBehandlingstema_returnererSEDForespørselBehandlingstema() throws MelosysException{
        behandling.setTema(ØVRIGE_SED_MED);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);
        when(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler)).thenReturn(true);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id, saksbehandler);
        assertThat(BEHANDLINGSTEMA_SED_FORESPØRSEL).isEqualTo(muligeBehandlingstema);
    }

    @Test
    void hentMuligeBehandlingstema_ugyldigBehandlingstema_returnererTomListe() throws MelosysException{
        behandling.setTema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);
        when(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler)).thenReturn(true);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id,saksbehandler);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_inaktivBehandling_returnererTomListe() throws MelosysException{
        behandling.setTema(ARBEID_FLERE_LAND);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id, saksbehandler);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_ikkeBehandlingRedigerbarOgTilordnetSaksbehandler_returnererTomListe() throws MelosysException{
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler)).thenReturn(false);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id, saksbehandler);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_erArtikkel16MedSendtAnmodningOmUnntak_returnererTomListe() throws MelosysException{
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);
        when(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler)).thenReturn(true);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id, saksbehandler);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void endreBehandlingstema_gyldigEndringForSøknad_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() throws MelosysException {
        behandling.setTema(ARBEID_FLERE_LAND);
        setup_endreBehandlingstemaTester();

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, UTSENDT_ARBEIDSTAKER, saksbehandler);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
        assertThat(oppgaveOppdateringArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
    }

    @Test
    void endreBehandlingstema_gyldigEndringForSED_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() throws MelosysException {
        behandling.setTema(TRYGDETID);
        setup_endreBehandlingstemaTester();

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED, saksbehandler);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
        assertThat(oppgaveOppdateringArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(ØVRIGE_SED_MED);
    }

    @Test
    void endreBehandlingstema_ugyldigNyttTemaForSøknad_exceptionKastes()  throws MelosysException{
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler)).thenReturn(true);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED, saksbehandler))
            .withMessage("Ikke mulig å endre behandlingstema");
        verify(behandlingService, never()).lagre(any(Behandling.class));
        verify(behandlingsresultatService, never()).tømBehandlingsresultat(id);
        verify(oppgaveService, never()).oppdaterOppgave(any(), any());
    }

    void setup_endreBehandlingstemaTester() throws MelosysException{
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnummer");
        behandling.setFagsak(fagsak);
        Oppgave oppgave = new Oppgave.Builder()
            .setOppgaveId("oppgaveID")
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .build();
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);
        when(oppgaveService.finnOppgaveMedFagsaksnummer(fagsak.getSaksnummer())).thenReturn(Optional.of(oppgave));
        when(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, saksbehandler)).thenReturn(true);
    }

}
