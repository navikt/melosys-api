package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    private ApplicationEventPublisher applicationEventPublisher;

    private DokumentServiceFasade dokumentServiceFasade;

    @BeforeEach
    void init() {
        dokumentServiceFasade = new DokumentServiceFasade(mockDokumentService, mockDokumentSystemService,
            mockDokgenService, mockBehandlingService, applicationEventPublisher);
        Mockito.reset(
            mockDokgenService,
            mockDokumentService,
            mockDokumentSystemService,
            mockBehandlingService
        );
    }

    @Test
    void skalKalleDokgenProduserUtkast() {
        when(mockDokgenService.erTilgjengeligDokgenmal(any(Produserbaredokumenter.class))).thenReturn(true);

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .medMottaker(Aktoersroller.BRUKER)
            .build();
        dokumentServiceFasade.produserUtkast(1, brevbestillingDto);

        verify(mockDokgenService).produserUtkast(anyLong(), any());
        verifyNoInteractions(mockDokumentService);
    }

    @Test
    void skalKalleDokumentServiceProduserUtkast() {
        when(mockDokgenService.erTilgjengeligDokgenmal(any(Produserbaredokumenter.class))).thenReturn(false);

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID)
            .build();

        dokumentServiceFasade.produserUtkast(1, brevbestillingDto);

        verify(mockDokumentService).produserUtkast(anyLong(), eq(brevbestillingDto));
    }

    @Test
    void skalKalleDokumentServiceProduserDokument() {
        when(mockDokgenService.erTilgjengeligDokgenmal(any())).thenReturn(false);

        dokumentServiceFasade.produserDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID, Mottaker.av(Aktoersroller.BRUKER), 123L, new DoksysBrevbestilling.Builder().build());

        verify(mockDokumentSystemService).produserDokument(any(), any(), anyLong(), any());
    }

    @Test
    void skalKalleDokgenServiceProduserOgDistribuer() {
        when(mockDokgenService.erTilgjengeligDokgenmal(any())).thenReturn(true);

        dokumentServiceFasade.produserDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID, Mottaker.av(Aktoersroller.BRUKER), 123L, new DoksysBrevbestilling.Builder().build());

        verify(mockDokgenService).produserOgDistribuerBrev(anyLong(), any());
        verifyNoInteractions(mockDokumentService);
    }

    @Test
    void skalKalleDokgenServiceProduserOgDistribuer_dto() {
        when(mockDokgenService.erTilgjengeligDokgenmal(any())).thenReturn(true);

        dokumentServiceFasade.produserDokument(1, new BrevbestillingDto());

        verify(mockDokgenService).produserOgDistribuerBrev(anyLong(), any());
        verifyNoInteractions(mockDokumentService);
    }
}
