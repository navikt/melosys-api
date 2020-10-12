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
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;

@RunWith(MockitoJUnitRunner.class)
public class EndreBehandlingstemaServiceTest {

    @Mock
    private BehandlingService behandlingService;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private EndreBehandlingstemaService endreBehandlingstemaService;

    @Captor
    private ArgumentCaptor<Behandling> behandlingArgumentCaptor;

    @Before
    public void setUp(){
        endreBehandlingstemaService = new EndreBehandlingstemaService(behandlingService, behandlingsresultatService);
    }

    @Test
    public void hentMuligeBehandlingstemaForSoknad() throws IkkeFunnetException {
        long id = 11L;
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(ARBEID_FLERE_LAND);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(MULIGE_BEHANDLINGSTEMA_SOKNAD).isEqualTo(muligeBehandlingstema);
    }

    @Test
    public void hentMuligeBehandlingstemaForSED() throws IkkeFunnetException{
        long id = 11L;
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(ØVRIGE_SED_MED);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

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
    public void hentMuligeBehandlingstemaErArtikkel16MedSendtAnmodningOmUnntak() throws IkkeFunnetException{
        long id = 11L;
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(ARBEID_FLERE_LAND);
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema.size()).isZero();
    }

    @Test
    public void endreBehandlingstemaSoknad() throws FunksjonellException {
        long id = 11L;
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, UTSENDT_ARBEIDSTAKER);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
    }

    @Test
    public void endreBehandlingstemaSED() throws FunksjonellException{
        long id = 11L;
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(TRYGDETID);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
    }

    @Test
    public void endreBehandlingstemaUgyldig()  throws FunksjonellException{
        long id = 11L;
        Behandling behandling = new Behandling();
        behandling.setId(id);
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        assertThrows(FunksjonellException.class, () -> endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED));
        verify(behandlingService, never()).lagre(any(Behandling.class));
        verify(behandlingsresultatService, never()).tømBehandlingsresultat(id);
    }
}
