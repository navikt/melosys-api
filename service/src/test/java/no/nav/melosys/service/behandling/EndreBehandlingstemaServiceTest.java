package no.nav.melosys.service.behandling;

import static no.nav.melosys.domain.Behandling.MULIGE_BEHANDLINGSTEMA_SED;
import static no.nav.melosys.domain.Behandling.MULIGE_BEHANDLINGSTEMA_SOKNAD;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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

@RunWith(MockitoJUnitRunner.class)
public class EndreBehandlingstemaServiceTest {

    private static final long id = 11L;

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

    @Before
    public void setUp(){
        endreBehandlingstemaService = new EndreBehandlingstemaService(behandlingService, behandlingsresultatService, oppgaveService);
    }

    @Test
    public void hentMuligeBehandlingstemaForSoknad() throws MelosysException {
        preTestEndreBehandlingstema(ARBEID_FLERE_LAND, new Behandlingsresultat());

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(MULIGE_BEHANDLINGSTEMA_SOKNAD).isEqualTo(muligeBehandlingstema);
    }

    @Test
    public void hentMuligeBehandlingstemaForSED() throws MelosysException{
        preTestEndreBehandlingstema(ØVRIGE_SED_MED, new Behandlingsresultat());

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(MULIGE_BEHANDLINGSTEMA_SED).isEqualTo(muligeBehandlingstema);
    }

    @Test
    public void hentMuligeBehandlingstemaUgyldigBehandlingstema() throws IkkeFunnetException{
        long id = 11L;
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema.size()).isZero();
    }

    @Test
    public void hentMuligeBehandlingstemaInaktivBehandling() throws IkkeFunnetException{
        long id = 11L;
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(ARBEID_FLERE_LAND);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema.size()).isZero();
    }

    @Test
    public void hentMuligeBehandlingstemaErArtikkel16MedSendtAnmodningOmUnntak() throws MelosysException{
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        preTestEndreBehandlingstema(ARBEID_FLERE_LAND, behandlingsresultat);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema.size()).isZero();
    }

    @Test
    public void endreBehandlingstemaSoknad() throws MelosysException {
        preTestEndreBehandlingstema(ARBEID_FLERE_LAND, new Behandlingsresultat());

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, UTSENDT_ARBEIDSTAKER);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
    }

    @Test
    public void endreBehandlingstemaSED() throws MelosysException {
        preTestEndreBehandlingstema(TRYGDETID, new Behandlingsresultat());

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
    }

    @Test
    public void endreBehandlingstemaUgyldig()  throws MelosysException{
        preTestEndreBehandlingstema(ARBEID_FLERE_LAND, new Behandlingsresultat());

        assertThrows(FunksjonellException.class, () -> endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED));
        verify(behandlingService, never()).lagre(any(Behandling.class));
        verify(behandlingsresultatService, never()).tømBehandlingsresultat(id);
    }
    @Test
    public void endreBehandlingstemaGosysOppgaveOppdateres() throws MelosysException{
        preTestEndreBehandlingstema(ARBEID_FLERE_LAND, new Behandlingsresultat());

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, UTSENDT_ARBEIDSTAKER);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringArgumentCaptor.capture());
        assertThat(oppgaveOppdateringArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
    }

    private void preTestEndreBehandlingstema(Behandlingstema behandlingstema, Behandlingsresultat behandlingsresultat) throws MelosysException{
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(behandlingstema);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnummer");
        behandling.setFagsak(fagsak);
        Oppgave oppgave = new Oppgave.Builder()
            .setOppgaveId("oppgaveID")
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .build();

        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);
        when(oppgaveService.finnOppgaveMedFagsaksnummer(fagsak.getSaksnummer())).thenReturn(Optional.of(oppgave));
    }
}
