package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Comparators;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.dokument.JournalpostInfoDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DokumentTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(DokumentTjenesteTest.class);

    private DokumentTjeneste dokumentTjeneste;

    @Mock
    private DokumentService dokumentService;
    @Mock
    private DokumentHentingService dokumentHentingService;
    @Mock
    private EessiService eessiService;
    @Mock
    private TilgangService tilgangService;

    @Before
    public void setUp() {
        dokumentTjeneste = new DokumentTjeneste(dokumentService, dokumentHentingService, eessiService, tilgangService);
    }

    @Test
    public void hentDokumenter() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException, IOException {
        List<Journalpost> journalposter = defaultEasyRandom().objects(Journalpost.class, 3).collect(Collectors.toList());
        given(dokumentHentingService.hentDokumenter(anyString())).willReturn(journalposter);

        ResponseEntity response = dokumentTjeneste.hentDokumenter("MEL-1873");
        @SuppressWarnings("unchecked")
        List<JournalpostInfoDto> dtos = (List<JournalpostInfoDto>) response.getBody();
        boolean inOrder = Comparators.isInOrder(dtos, Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));
        assertThat(inOrder).isTrue();

        validerArray(dtos, "dokumenter-oversikt-schema.json", log);
    }

    @Test
    public void hentBrevForhåndsvisning() throws MelosysException, IOException {
        final byte[] MOCK_PDF = "bytes fra et brev".getBytes();
        when(dokumentService.produserUtkast(any(), anyLong(), any())).thenReturn(MOCK_PDF);
        BrevbestillingDto brevBestillingDto = new BrevbestillingDto();

        ResponseEntity response = dokumentTjeneste.produserUtkastBrev(1L, Produserbaredokumenter.ATTEST_A1, brevBestillingDto);
        assertThat(response.getBody()).isEqualTo(MOCK_PDF);
        valider(brevBestillingDto, "dokumenter-pdf-utkast-brev-post-schema.json");
    }

    @Test
    public void hentSedForhåndsvisning() throws MelosysException, IOException {
        final byte[] MOCK_PDF = "bytes fra en pdf".getBytes();
        when(eessiService.genererSedPdf(anyLong(), any(), any())).thenReturn(MOCK_PDF);
        SedPdfData sedPdfData = new SedPdfData("tada", null, "DK", "neida");

        ResponseEntity response = dokumentTjeneste.produserUtkastSed(1L, SedType.A001, sedPdfData);
        assertThat(response.getBody()).isEqualTo(MOCK_PDF);
        valider(sedPdfData, "dokumenter-pdf-utkast-sed-post-schema.json");
    }
}