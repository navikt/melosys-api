package no.nav.melosys.saksflyt.steg.behandling;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettVurderDokumentTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;

    private SettVurderDokument settVurderDokument;

    private final long behandlingID = 21321L;
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @BeforeEach
    public void setUp() {
        settVurderDokument = new SettVurderDokument(fagsakService, behandlingService);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER);
    }

    @Test
    void utfør_sakMedBehandling_oppdatererStatus() {
        when(fagsakService.hentFagsak(eq(FagsakTestFactory.SAKSNUMMER))).thenReturn(fagsakMedBehandling());
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService).endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT);
    }

    @Test
    void utfør_sakUtenBehandling_ingenStatusEndring() {
        when(fagsakService.hentFagsak(eq(FagsakTestFactory.SAKSNUMMER))).thenReturn(FagsakTestFactory.lagFagsak());
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService, never()).endreStatus(anyLong(), any());
    }

    @Test
    void utfør_ingenVurdering_ingenStatusEndring() {
        when(fagsakService.hentFagsak(eq(FagsakTestFactory.SAKSNUMMER))).thenReturn(fagsakMedBehandling());
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, true);
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService, never()).endreStatus(anyLong(), any());
    }

    private Fagsak fagsakMedBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        return FagsakTestFactory.builder().behandlinger(behandling).build();
    }
}
