package no.nav.melosys.saksflyt.felles;

import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettSakRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FagsakOgBehandlingFellesTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private FagsakOgBehandlingFelles fagsakOgBehandlingFelles;

    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Before
    public void setup() {
        fagsakOgBehandlingFelles = new FagsakOgBehandlingFelles(fagsakService, behandlingService, behandlingsresultatRepository);
        when(behandlingsresultatRepository.findById(any())).thenReturn(Optional.of(behandlingsresultat));
    }

    @Test
    public void avsluttFagsakOgBehandling_forventMetodekall() throws Exception {

        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        behandling.setId(1L);

        fagsakOgBehandlingFelles.avsluttFagsakOgBehandling(behandling, Behandlingsresultattyper.REGISTRERT_UNNTAK);

        verify(fagsakService).lagre(any(Fagsak.class));
        verify(behandlingService).oppdaterStatus(eq(1L), eq(Behandlingsstatus.AVSLUTTET));
        verify(behandlingsresultatRepository).findById(1L);
        verify(behandlingsresultatRepository).save(any(Behandlingsresultat.class));

        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.REGISTRERT_UNNTAK);
        assertThat(behandlingsresultat.getUtfallRegistreringUnntak()).isEqualTo(UtfallRegistreringUnntak.GODKJENT);
    }

    @Test
    public void opprettFagsakOgBehandling_forventMetodekall() throws FunksjonellException {
        String aktørId = "123";
        Behandlingstyper behandlingstype = Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL;
        String journalpostId = "abcdefg";
        String dokumentId = "987";
        long gsakSaksnummer = 456L;

        fagsakOgBehandlingFelles.opprettFagsakOgBehandling(aktørId, behandlingstype, journalpostId, dokumentId, gsakSaksnummer, Sakstyper.EU_EOS);

        verify(fagsakService).nyFagsakOgBehandling(any(OpprettSakRequest.class));
    }

    @Test
    public void opprettBehandlingPåEksisterendeFagsak_medAktivBehandling_forventMetodekall() throws TekniskException, IkkeFunnetException {
        Fagsak fagsak = new Fagsak();
        Behandling aktivBehandling = new Behandling();
        aktivBehandling.setId(1L);
        aktivBehandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(Collections.singletonList(aktivBehandling));

        Behandlingsstatus behandlingsstatus = Behandlingsstatus.OPPRETTET;
        Behandlingstyper behandlingstype = Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL;
        String journalpostId = "abcdefg";
        String dokumentId = "987";
        long gsakSaksnummer = 456L;

        fagsakOgBehandlingFelles.opprettBehandlingPåEksisterendeFagsak(fagsak, behandlingsstatus, behandlingstype, journalpostId, dokumentId, gsakSaksnummer);

        verify(behandlingService).avsluttBehandling(eq(1L));
        verify(behandlingService).nyBehandling(eq(fagsak), eq(behandlingsstatus), eq(behandlingstype), eq(journalpostId), eq(dokumentId));
    }
}