package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
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
    private Prosessinstans prosessinstans;

    @BeforeEach
    public void setUp() {
        settVurderDokument = new SettVurderDokument(fagsakService, behandlingService);
        prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            .build();
    }

    @Test
    void utfør_sakMedBehandling_oppdatererStatus() {
        when(fagsakService.hentFagsak(eq(FagsakTestFactory.SAKSNUMMER))).thenReturn(fagsakMedBehandling());
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.JFR_INGEN_VURDERING, false)
            .build();
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService).endreStatus(behandlingID, Behandlingsstatus.VURDER_DOKUMENT);
    }

    @Test
    void utfør_sakUtenBehandling_ingenStatusEndring() {
        when(fagsakService.hentFagsak(eq(FagsakTestFactory.SAKSNUMMER))).thenReturn(FagsakTestFactory.lagFagsak());
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.JFR_INGEN_VURDERING, false)
            .build();
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService, never()).endreStatus(anyLong(), any());
    }

    @Test
    void utfør_ingenVurdering_ingenStatusEndring() {
        when(fagsakService.hentFagsak(eq(FagsakTestFactory.SAKSNUMMER))).thenReturn(fagsakMedBehandling());
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.JFR_INGEN_VURDERING, true)
            .build();
        settVurderDokument.utfør(prosessinstans);
        verify(behandlingService, never()).endreStatus(anyLong(), any());
    }

    private Fagsak fagsakMedBehandling() {
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(behandlingID)
            .medStatus(Behandlingsstatus.UNDER_BEHANDLING)
            .build();

        return FagsakTestFactory.builder().behandlinger(behandling).build();
    }}
