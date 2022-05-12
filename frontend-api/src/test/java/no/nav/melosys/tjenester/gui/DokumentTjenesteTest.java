package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Comparators;
import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentServiceFasade;
import no.nav.melosys.service.dokument.brev.SedPdfData;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.brev.BrevbestillingDto;
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
class DokumentTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(DokumentTjenesteTest.class);

    private DokumentTjeneste dokumentTjeneste;

    @Mock
    private DokumentServiceFasade dokumentServiceFasade;
    @Mock
    private DokumentHentingService dokumentHentingService;
    @Mock
    private EessiService eessiService;
    @Mock
    private Aksesskontroll aksesskontroll;

    @BeforeEach
    public void setUp() {
        dokumentTjeneste = new DokumentTjeneste(dokumentServiceFasade, dokumentHentingService, eessiService, aksesskontroll);
    }

    @Test
    void hentDokumenter() throws Exception {
        List<Journalpost> journalposter = lagJournalposter();
        given(dokumentHentingService.hentJournalposter(anyString())).willReturn(journalposter);

        ResponseEntity<List<JournalpostInfoDto>> response = dokumentTjeneste.hentDokumenter("MEL-1873");
        List<JournalpostInfoDto> dtos = response.getBody();
        assertThat(dtos).isNotNull();
        boolean inOrder = Comparators.isInOrder(dtos, Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));
        assertThat(inOrder).isTrue();

        validerArray(dtos, "dokumenter-oversikt-schema.json", log);
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

    private static List<Journalpost> lagJournalposter() {
        return Stream.generate(DokumentTjenesteTest::lagJournalpost).limit(3).collect(Collectors.toList());
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
