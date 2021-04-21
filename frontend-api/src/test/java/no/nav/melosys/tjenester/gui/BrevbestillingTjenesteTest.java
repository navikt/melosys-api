package no.nav.melosys.tjenester.gui;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.BrevbestillingService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.tjenester.gui.dto.brev.BrevmalDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MANGELBREV_BRUKER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrevbestillingTjenesteTest extends JsonSchemaTestParent {

    @Mock
    private BehandlingService mockBehandlingService;

    @Mock
    private AvklarteVirksomheterService mockAvklarteVirksomheterService;

    @Mock
    private DokumentServiceFasade mockDokServiceFasade;

    @Mock
    private DokgenService mockDokgenService;

    private BrevbestillingTjeneste brevbestillingTjeneste;

    @BeforeEach
    void init() {
        BrevbestillingService brevbestillingService = new BrevbestillingService(mockBehandlingService, mockAvklarteVirksomheterService, mockDokServiceFasade, mockDokgenService);
        brevbestillingTjeneste = new BrevbestillingTjeneste(brevbestillingService);
    }

    @Test
    void skalReturnereTilgjengeligeBrevmaler() throws Exception {
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(new Behandling());

        List<BrevmalDto> brevmaler = brevbestillingTjeneste.hentTilgjengeligeMaler(123L);
        assertEquals(2, brevmaler.size());
    }

    @Test
    void skalReturnereUtkast() throws Exception {
        byte[] forventetPdf = "UTKAST".getBytes(StandardCharsets.UTF_8);
        when(mockDokServiceFasade.produserUtkast(any(), anyLong(), any())).thenReturn(forventetPdf);

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .medInnledningFritekst("Innledning")
            .medManglerFritekst("Mangler")
            .build();
        ResponseEntity<byte[]> responseEntity = brevbestillingTjeneste.produserUtkast(123L, MANGELBREV_BRUKER, brevbestillingDto);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(forventetPdf, responseEntity.getBody());

        verifyNoInteractions(mockDokgenService);

        valider(brevbestillingDto, "dokumenter-v2-utkast-post-schema.json", new ObjectMapper());
    }

    @Test
    void skalBestilleProduseringAvBrev() throws Exception {
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .medInnledningFritekst("Innledning")
            .medManglerFritekst("Mangler")
            .build();
        brevbestillingTjeneste.produserBrev(123L, MANGELBREV_BRUKER, brevbestillingDto);

        verify(mockDokgenService).produserOgDistribuerBrev(eq(MANGELBREV_BRUKER), eq(123L), eq(brevbestillingDto));
        verifyNoInteractions(mockDokServiceFasade);

        valider(brevbestillingDto, "dokumenter-v2-utkast-post-schema.json", new ObjectMapper());
    }

}
