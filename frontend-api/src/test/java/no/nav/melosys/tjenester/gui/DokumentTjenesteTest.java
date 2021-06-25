package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Comparators;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.JournalpostInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DokumentTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(DokumentTjenesteTest.class);

    private DokumentTjeneste dokumentTjeneste;

    @Mock
    private DokumentServiceFasade dokumentServiceFasade;
    @Mock
    private DokumentHentingService dokumentHentingService;
    @Mock
    private EessiService eessiService;
    @Mock
    private TilgangService tilgangService;

    @BeforeEach
    public void setUp() {
        dokumentTjeneste = new DokumentTjeneste(dokumentServiceFasade, dokumentHentingService, eessiService, tilgangService);
    }

    @Test
    void hentDokumenter() throws Exception {
        List<Journalpost> journalposter = lagJournalPoster(3);
        given(dokumentHentingService.hentDokumenter(anyString())).willReturn(journalposter);

        ResponseEntity<List<JournalpostInfoDto>> response = dokumentTjeneste.hentDokumenter("MEL-1873");
        List<JournalpostInfoDto> dtos = response.getBody();
        assertThat(dtos).isNotNull();
        boolean inOrder = Comparators.isInOrder(dtos, Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));
        assertThat(inOrder).isTrue();

        validerArray(dtos, "dokumenter-oversikt-schema.json", log);
    }

    @Test
    void hentBrevForhåndsvisning() throws IOException {
        final byte[] MOCK_PDF = "bytes fra et brev".getBytes();
        when(dokumentServiceFasade.produserUtkast(anyLong(), any())).thenReturn(MOCK_PDF);
        BrevbestillingDto brevBestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(Produserbaredokumenter.ATTEST_A1)
            .medBegrunnelseKode("KODE")
            .medFritekst("Fritekst.")
            .medMottaker(Aktoersroller.MYNDIGHET)
            .medYtterligereInformasjon("Ytterligere informasjon")
            .build();

        ResponseEntity response = dokumentTjeneste.produserUtkastBrev(1L, Produserbaredokumenter.ATTEST_A1, brevBestillingDto);
        assertThat(response.getBody()).isEqualTo(MOCK_PDF);
        valider(brevBestillingDto, "dokumenter-v2-utkast-post-schema.json", new ObjectMapper());
    }

    @Test
    void hentSedForhåndsvisning() throws IOException {
        final byte[] MOCK_PDF = "bytes fra en pdf".getBytes();
        when(eessiService.genererSedPdf(anyLong(), any(), any())).thenReturn(MOCK_PDF);
        SedPdfData sedPdfData = new SedPdfData("tada", null, "DK", "neida");

        ResponseEntity response = dokumentTjeneste.produserUtkastSed(1L, SedType.A001, sedPdfData);
        assertThat(response.getBody()).isEqualTo(MOCK_PDF);
        valider(sedPdfData, "dokumenter-pdf-utkast-sed-post-schema.json");
    }

    @Test
    void produserDokument() {
        BrevbestillingDto brevBestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        dokumentTjeneste.produserDokument(1L,
            Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, brevBestillingDto);

        verify(dokumentServiceFasade).produserUtkast(anyLong(), any());
        verify(dokumentServiceFasade).produserDokument(anyLong(), any());
    }

    @Test
    void produserDokumentFeilerMedManglendeMottaker() {
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> dokumentTjeneste.produserDokument(1L,
                Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, new BrevbestillingDto()))
            .withMessageContaining("Mottaker trengs for å bestille");
    }

    private static List<Journalpost> lagJournalPoster(int antall) {
        return Stream.generate(DokumentTjenesteTest::lagJournalpost).limit(antall).collect(Collectors.toList());
    }

    private static Journalpost lagJournalpost() {
        Journalpost journalpost = new Journalpost("jpID");
        journalpost.setJournalposttype(Journalposttype.UT);
        journalpost.setAvsenderId("nav");
        journalpost.setAvsenderId("NAVAT:07");
        journalpost.setKorrespondansepartNavn("Test12345");
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId("2456");
        arkivDokument.setTittel("Tittel 234");

        journalpost.setHoveddokument(arkivDokument);

        return journalpost;
    }

}
