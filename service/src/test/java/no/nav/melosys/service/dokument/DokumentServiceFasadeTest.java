package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DokumentServiceFasadeTest {

    @Mock
    private DokumentService mockDokumentService;
    @Mock
    private DokumentSystemService mockDokumentSystemService;
    @Mock
    private DokgenService mockDokgenService;
    @Mock
    private BehandlingService mockBehandlingService;
    @Mock
    private ProsessinstansService mockProsessinstansService;

    private DokumentServiceFasade dokumentServiceFasade;

    @BeforeEach
    void init() {
        dokumentServiceFasade = new DokumentServiceFasade(mockDokumentService, mockDokumentSystemService,
            mockDokgenService, mockBehandlingService, mockProsessinstansService);
        Mockito.reset(
            mockDokgenService,
            mockDokumentService,
            mockDokumentSystemService,
            mockBehandlingService,
            mockProsessinstansService
        );
    }

    @Test
    void skalKalleDokgenProduserUtkast() throws Exception {
        when(mockDokgenService.erTilgjengeligDokgenmal(any(Produserbaredokumenter.class))).thenReturn(true);

        dokumentServiceFasade.produserUtkast(MELDING_FORVENTET_SAKSBEHANDLINGSTID, 1, new BrevbestillingDto());

        verify(mockDokgenService).produserUtkast(any(), anyLong(), any());
        verifyNoInteractions(mockDokumentService);
    }

    @Test
    void skalKalleDokumentServiceProduserUtkast() throws Exception {
        when(mockDokgenService.erTilgjengeligDokgenmal(any(Produserbaredokumenter.class))).thenReturn(false);

        dokumentServiceFasade.produserUtkast(MELDING_FORVENTET_SAKSBEHANDLINGSTID, 1, new BrevbestillingDto());

        verify(mockDokumentService).produserUtkast(any(), anyLong(), any());
    }

    @Test
    void skalKalleDokumentServiceProduserDokument() throws Exception {
        when(mockDokgenService.erTilgjengeligDokgenmal(any())).thenReturn(false);

        dokumentServiceFasade.produserDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID, Mottaker.av(Aktoersroller.BRUKER), 1, new Brevbestilling.Builder().build());

        verify(mockDokumentSystemService).produserDokument(any(), any(), anyLong(), any());
    }

    @Test
    void skalKalleProsessinstansServiceProduserDokument() throws Exception {
        when(mockDokgenService.erTilgjengeligDokgenmal(any())).thenReturn(true);

        dokumentServiceFasade.produserDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID, Mottaker.av(Aktoersroller.BRUKER), 1, new Brevbestilling.Builder().build());

        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(), any());
        verifyNoInteractions(mockDokumentService);
    }

    @Test
    void skalKalleProsessinstansServiceProduserDokument_dto() throws Exception {
        when(mockDokgenService.erTilgjengeligDokgenmal(any())).thenReturn(true);

        dokumentServiceFasade.produserDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID,1, new BrevbestillingDto());

        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(), any());
        verifyNoInteractions(mockDokumentService);
    }
}