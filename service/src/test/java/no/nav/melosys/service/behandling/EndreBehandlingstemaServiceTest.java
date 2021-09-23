package no.nav.melosys.service.behandling;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SED_FORESPØRSEL;
import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SØKNAD;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndreBehandlingstemaServiceTest {

    private static final long id = 11L;
    private final Behandling behandling = new Behandling();
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Captor
    private ArgumentCaptor<Behandling> behandlingArgumentCaptor;
    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveOppdateringArgumentCaptor;
    @Captor
    private ArgumentCaptor<Behandlingsgrunnlag> behandlingsgrunnlagArgumentCaptor;


    private EndreBehandlingstemaService endreBehandlingstemaService;

    @BeforeEach
    void setUp() {
        endreBehandlingstemaService = new EndreBehandlingstemaService(
            behandlingService,
            behandlingsresultatService,
            oppgaveService,
            behandlingsgrunnlagService
        );

        behandling.setId(id);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(id)).thenReturn(behandling);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSøknadBehandlingstema_returnererSøknadBehandlingstema() {
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(BEHANDLINGSTEMA_SØKNAD).isEqualTo(muligeBehandlingstema);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSEDForespørselBehandlingstema_returnererSEDForespørselBehandlingstema() {
        behandling.setTema(ØVRIGE_SED_MED);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(BEHANDLINGSTEMA_SED_FORESPØRSEL).isEqualTo(muligeBehandlingstema);
    }

    @Test
    void hentMuligeBehandlingstema_ugyldigBehandlingstema_returnererTomListe() {
        behandling.setTema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_inaktivBehandling_returnererTomListe() {
        behandling.setTema(ARBEID_FLERE_LAND);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_erArtikkel16MedSendtAnmodningOmUnntak_returnererTomListe() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingstemaService.hentMuligeBehandlingstema(id);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void endreBehandlingstema_gyldigEndringForSøknad_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() {
        behandling.setTema(ARBEID_FLERE_LAND);
        setup_endreBehandlingstemaTester();
        Oppgave behandlingsOppgaveForType = OppgaveFactory.lagBehandlingsOppgaveForType(UTSENDT_ARBEIDSTAKER, behandling.getType()).build();


        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, UTSENDT_ARBEIDSTAKER);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
        assertThat(oppgaveOppdateringArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(behandlingsOppgaveForType.getBehandlingstema());
    }

    @Test
    void endreBehandlingstema_gyldigEndringForSED_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() {
        behandling.setTema(TRYGDETID);
        setup_endreBehandlingstemaTester();
        Oppgave behandlingsOppgaveForType = OppgaveFactory.lagBehandlingsOppgaveForType(ØVRIGE_SED_MED, behandling.getType()).build();

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED);
        verify(behandlingService).lagre(behandlingArgumentCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(id);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingArgumentCaptor.getValue().getId()).isEqualTo(id);
        assertThat(oppgaveOppdateringArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(behandlingsOppgaveForType.getBehandlingstema());
    }

    @Test
    void endreBehandlingstema_ugyldigNyttTemaForSøknad_exceptionKastes() {
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, ØVRIGE_SED_MED))
            .withMessage("Ikke mulig å endre behandlingstema");
        verify(behandlingService, never()).lagre(any(Behandling.class));
        verify(behandlingsresultatService, never()).tømBehandlingsresultat(id);
        verify(oppgaveService, never()).oppdaterOppgave(any(), any());
    }

    @Test
    void endreBehandlingstema_nyttTemaErIkkeArbeidIFlereLand_erUkjenteEllerAlleEosLandSettesTilFalse() {
        behandling.setTema(ARBEID_FLERE_LAND);
        setup_endreBehandlingstemaTester();

        endreBehandlingstemaService.endreBehandlingstemaTilBehandling(id, UTSENDT_ARBEIDSTAKER);

        verify(behandlingsgrunnlagService).oppdaterBehandlingsgrunnlag(behandlingsgrunnlagArgumentCaptor.capture());
        assertThat(behandlingsgrunnlagArgumentCaptor.getValue().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand).isFalse();
    }

    void setup_endreBehandlingstemaTester() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnummer");
        behandling.setFagsak(fagsak);
        Oppgave oppgave = new Oppgave.Builder()
            .setOppgaveId("oppgaveID")
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .build();
        when(behandlingsresultatService.hentBehandlingsresultat(id)).thenReturn(behandlingsresultat);
        when(oppgaveService.finnÅpenOppgaveMedFagsaksnummer(fagsak.getSaksnummer())).thenReturn(Optional.of(oppgave));
    }
}
