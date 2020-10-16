package no.nav.melosys.service.behandling;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SED_FORESPØRSEL;
import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SØKNAD;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveService;

@ExtendWith(MockitoExtension.class)
public class EndreBehandlingstemaServiceTest {

    private static final long id = 11L;
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
    public void setUp() throws MelosysException {
        endreBehandlingstemaService = new EndreBehandlingstemaService(behandlingService, behandlingsresultatService, oppgaveService);

        behandling.setId(id);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnummer");
        behandling.setFagsak(fagsak);
        Oppgave oppgave = new Oppgave.Builder()
            .setOppgaveId("oppgaveID")
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .build();

        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
        lenient().when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);
        lenient().when(oppgaveService.finnOppgaveMedFagsaksnummer(fagsak.getSaksnummer())).thenReturn(Optional.of(oppgave));
    }

    @Test
    public void hentMuligeBehandlingstema_gyldigSøknadBehandlingstema_returnererSøknadBehandlinstema() throws MelosysException {
        behandling.setTema(ARBEID_FLERE_LAND);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(BEHANDLINGSTEMA_SØKNAD).isEqualTo(muligeBehandlingstema);
    }

    @Test
    public void hentMuligeBehandlingstema_gyldigSEDForespørselBehandlingstema_returnererSEDForespørselBehandlingstema() throws MelosysException{
        behandling.setTema(ØVRIGE_SED_MED);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(BEHANDLINGSTEMA_SED_FORESPØRSEL).isEqualTo(muligeBehandlingstema);
    }

    @Test
    public void hentMuligeBehandlingstema_ugyldigBehandlingstema_returnererTomListe() throws IkkeFunnetException{
        behandling.setTema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema.size()).isZero();
    }

    @Test
    public void hentMuligeBehandlingstema_inaktivBehandling_returnererTomListe() throws IkkeFunnetException{
        behandling.setTema(ARBEID_FLERE_LAND);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema.size()).isZero();
    }

    @Test
    public void hentMuligeBehandlingstema_erArtikkel16MedSendtAnmodningOmUnntak_returnererTomListe() throws MelosysException{
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        behandling.setTema(ARBEID_FLERE_LAND);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema.size()).isZero();
    }

    @Test
    public void endreBehandlingstema_gyldigEndringForSøknad_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() throws MelosysException {
        behandling.setTema(ARBEID_FLERE_LAND);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, UTSENDT_ARBEIDSTAKER);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
        assertThat(oppgaveOppdateringArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
    }

    @Test
    public void endreBehandlingstema_gyldigEndringForSED_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() throws MelosysException {
        behandling.setTema(TRYGDETID);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
        assertThat(oppgaveOppdateringArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(ØVRIGE_SED_MED);
    }

    @Test
    public void endreBehandlingstema_ugyldigNyttTemaForSøknad_exceptionKastes()  throws MelosysException{
        behandling.setTema(ARBEID_FLERE_LAND);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED))
            .withMessage("Ikke mulig å endre behandlingstema");
        verify(behandlingService, never()).lagre(any(Behandling.class));
        verify(behandlingsresultatService, never()).tømBehandlingsresultat(id);
        verify(oppgaveService, never()).oppdaterOppgave(any(), any());
    }

}
