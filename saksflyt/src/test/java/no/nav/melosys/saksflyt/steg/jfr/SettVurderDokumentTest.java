package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
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

    private final String saksnummer = "MEL-12345678";
    private final long behandlingID = 21321L;
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @BeforeEach
    public void setUp() {
        settVurderDokument = new SettVurderDokument(fagsakService, behandlingService);
        prosessinstans.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
    }

    @Test
    void utfør_sakMedBehandling_oppdatererStatus() throws FunksjonellException, TekniskException {
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsakMedBehandling());
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService).oppdaterStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT);
    }

    @Test
    void utfør_sakUtenBehandling_ingenStatusEndring() throws FunksjonellException, TekniskException {
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(new Fagsak());
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService, never()).oppdaterStatus(anyLong(), any());
    }

    @Test
    void utfør_ingenVurdering_ingenStatusEndring() throws FunksjonellException, TekniskException {
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsakMedBehandling());
        prosessinstans.setData(ProsessDataKey.JFR_INGEN_VURDERING, true);
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService, never()).oppdaterStatus(anyLong(), any());
    }

    private Fagsak fagsakMedBehandling() {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        return fagsak;
    }
}