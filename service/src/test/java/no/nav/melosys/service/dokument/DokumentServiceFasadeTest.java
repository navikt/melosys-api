package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static no.nav.melosys.domain.kodeverk.Mottakerroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DokumentServiceFasadeTest {

    @Mock
    private DokumentService mockDokumentService;
    @Mock
    private DokgenService mockDokgenService;
    @Mock
    private BehandlingService mockBehandlingService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Captor
    private ArgumentCaptor<BrevbestillingDto> brevbestillingRequestCaptor;

    private DokumentServiceFasade dokumentServiceFasade;

    @BeforeEach
    void init() {
        SpringSubjectHandler.set(new TestSubjectHandler());
        dokumentServiceFasade = new DokumentServiceFasade(mockDokumentService,
            mockDokgenService, mockBehandlingService, applicationEventPublisher);
        Mockito.reset(
            mockDokgenService,
            mockDokumentService,
            mockBehandlingService
        );
    }

    @Test
    void skalKalleDokumentServiceProduserDokument() {
        when(mockDokgenService.erTilgjengeligDokgenmal(any())).thenReturn(false);

        dokumentServiceFasade.produserDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID, Mottaker.medRolle(BRUKER), 123L, new DoksysBrevbestilling.Builder().build());

        verify(mockDokumentService).produserDokument(any(), any(), anyLong(), any());
    }

    @Test
    void skalKalleDokgenServiceProduserOgDistribuer() {
        when(mockDokgenService.erTilgjengeligDokgenmal(any())).thenReturn(true);

        dokumentServiceFasade.produserDokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID, Mottaker.medRolle(BRUKER), 123L, new DoksysBrevbestilling.Builder().build());

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

    @Test
    void skal_lageRiktigDokgenBrevRequest_ved_avslagManglendeOpplysninger_() {
        when(mockDokgenService.erTilgjengeligDokgenmal(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER)).thenReturn(true);

        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
            .medProduserbartDokument(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER)
            .medAvsenderID("Z123456")
            .medMottakere(List.of(Mottaker.medRolle(BRUKER)))
            .medFritekst("avslag fritekst")
            .build();

        dokumentServiceFasade.produserDokument(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER, Mottaker.medRolle(BRUKER), 1L, brevbestilling);

        verify(mockDokgenService).produserOgDistribuerBrev(eq(1L), brevbestillingRequestCaptor.capture());
        verifyNoInteractions(mockDokumentService);

        var dokgenBrevbestillingRequest = brevbestillingRequestCaptor.getValue();

        assertThat(dokgenBrevbestillingRequest).extracting(
            BrevbestillingDto::getBestillersId,
            BrevbestillingDto::getMottaker,
            BrevbestillingDto::getFritekst
        ).containsExactly("Z123456", BRUKER, "avslag fritekst");
    }

    @Test
    void skal_lageRiktigDokgenBrevRequest_ved_meldingHenleggSak_() {

        dokumentServiceFasade.produserOgDistribuerBrev(Produserbaredokumenter.MELDING_HENLAGT_SAK, Mottaker.medRolle(BRUKER),
            "henlagt sak fritekst", "ANNET", "Z123456", 1L);

        verify(mockDokgenService).produserOgDistribuerBrev(eq(1L), brevbestillingRequestCaptor.capture());
        verifyNoInteractions(mockDokumentService);

        var dokgenBrevbestillingRequest = brevbestillingRequestCaptor.getValue();

        assertThat(dokgenBrevbestillingRequest).extracting(
            BrevbestillingDto::getBestillersId,
            BrevbestillingDto::getMottaker,
            BrevbestillingDto::getFritekst,
            BrevbestillingDto::getBegrunnelseKode
        ).containsExactly("Z123456", BRUKER, "henlagt sak fritekst", "ANNET");
    }
}
