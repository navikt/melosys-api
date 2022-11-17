package no.nav.melosys.integrasjon.doksys;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.dokument.arbeidsforhold.Aktoertype;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.DistribuerJournalpostConsumer;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse;
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Dokumentbestillingsinformasjon;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.UtenlandskPostadresse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokSysServiceTest {

    private final String FNR = "12345678901";
    private final String ORGNR = "98765432";
    private final String INSITUSJON_ID = "DK:1234";
    private final String REP_FNR = "10987654321";
    private final String REP_ORGNR = "87654321";

    @Mock
    private DokumentproduksjonConsumer dokumentproduksjonConsumer;
    @Mock
    private DistribuerJournalpostConsumer distribuerJournalpostConsumer;

    private DoksysService dokSysService;

    @BeforeEach
    void setUp() {
        dokSysService = new DoksysService(dokumentproduksjonConsumer, distribuerJournalpostConsumer);
    }

    @Test
    void produserDokumentutkast() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataForBruker(null);
        when(dokumentproduksjonConsumer.produserDokumentutkast(any())).thenReturn(new ProduserDokumentutkastResponse());

        dokSysService.produserDokumentutkast(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserDokumentutkastRequest> captor = ArgumentCaptor.forClass(ProduserDokumentutkastRequest.class);
        verify(dokumentproduksjonConsumer).produserDokumentutkast(captor.capture());
        ProduserDokumentutkastRequest dokumentutkastRequest = captor.getValue();
        assertThat(dokumentutkastRequest.getDokumenttypeId()).isEqualTo(metadata.dokumenttypeID);
    }

    @Test
    void produserIkkeredigerbartDokument_forBruker() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataForBruker(null);
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());

        Dokumentbestillingsinformasjon dokInfo = hentDokumentBestillingInfoFraCaptor(captor);
        assertThat(dokInfo.getDokumenttypeId()).isEqualTo(metadata.dokumenttypeID);
        assertThat(dokInfo.getAdresse()).isNull();
        assertThat(dokInfo.getMottaker().isBerik()).isTrue();
        assertThat(dokInfo.getMottaker()).isInstanceOf(Person.class);
        assertThat(((Person) dokInfo.getBruker()).getIdent()).isEqualTo(FNR);
    }

    @Test
    void produserIkkeredigerbartDokument_forBrukerMedPostadresse_girPostadresseOgNavn() throws Exception {
        StrukturertAdresse postadresse = new StrukturertAdresse();
        postadresse.setGatenavn("Gatenavn");
        postadresse.setHusnummerEtasjeLeilighet("123");
        postadresse.setPostnummer("1337");
        postadresse.setPoststed("Poststed");
        postadresse.setRegion("Region");
        postadresse.setLandkode("BE");
        DokumentbestillingMetadata metadata = lagMetadataForBruker(postadresse);
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());

        Dokumentbestillingsinformasjon dokInfo = hentDokumentBestillingInfoFraCaptor(captor);
        assertThat(dokInfo.getMottaker().isBerik()).isFalse();

        UtenlandskPostadresse adresse = (UtenlandskPostadresse) dokInfo.getAdresse();
        assertThat(adresse.getAdresselinje1()).isEqualTo(
            postadresse.getGatenavn() + " " + postadresse.getHusnummerEtasjeLeilighet());
        assertThat(adresse.getAdresselinje2()).isEqualTo(postadresse.getPostnummer() + " " + postadresse.getPoststed());
        assertThat(adresse.getAdresselinje3()).isEqualTo(postadresse.getRegion());
        assertThat(adresse.getLand().getValue()).isEqualTo(postadresse.getLandkode());
        assertThat(((Person) dokInfo.getBruker()).getNavn()).isEqualTo("Kim Se");
    }

    @Test
    void produserIkkeredigerbartDokument_tilArbeidsgiver() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataMedArbeidsgiver();
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());

        Dokumentbestillingsinformasjon dokInfo = hentDokumentBestillingInfoFraCaptor(captor);
        assertThat(dokInfo.getMottaker()).isInstanceOf(Organisasjon.class);
        assertThat(((Organisasjon) dokInfo.getMottaker()).getOrgnummer()).isEqualTo(ORGNR);
    }

    @Test
    void produserIkkeredigerbartDokument_tilRepresentantPerson() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataMedRepresentant(Aktoertype.PERSON);
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());

        Dokumentbestillingsinformasjon dokInfo = hentDokumentBestillingInfoFraCaptor(captor);
        assertThat(dokInfo.getMottaker().isBerik()).isTrue();
        assertThat(dokInfo.getMottaker()).isInstanceOf(Person.class);
        assertThat(((Person) dokInfo.getMottaker()).getIdent()).isEqualTo(REP_FNR);
    }

    @Test
    void produserIkkeredigerbartDokument_tilRepresentantOrganisasjon() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataMedRepresentant(Aktoertype.ORGANISASJON);
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());

        Dokumentbestillingsinformasjon dokInfo = hentDokumentBestillingInfoFraCaptor(captor);
        assertThat(dokInfo.getMottaker()).isInstanceOf(Organisasjon.class);
        assertThat(((Organisasjon) dokInfo.getMottaker()).getOrgnummer()).isEqualTo(REP_ORGNR);
    }

    @Test
    void produserIkkeredigerbartDokument_tilUtenlandskMyndighet() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataMedMyndighet();
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());

        Dokumentbestillingsinformasjon dokInfo = hentDokumentBestillingInfoFraCaptor(captor);
        assertThat(dokInfo.getMottaker().isBerik()).isFalse();
        assertThat(dokInfo.getMottaker()).isInstanceOf(Person.class);
        assertThat(((Person) dokInfo.getMottaker()).getNavn()).isEqualTo(metadata.utenlandskMyndighet.navn);


        UtenlandskPostadresse utenlandskPostadresse = (UtenlandskPostadresse) dokInfo.getAdresse();
        assertThat(utenlandskPostadresse.getAdresselinje1()).isEqualTo(metadata.utenlandskMyndighet.gateadresse1);
        assertThat(utenlandskPostadresse.getLand().getValue()).isEqualTo(metadata.utenlandskMyndighet.landkode.getKode());
    }

    @Test
    void distribuerJournalpost_norskAdresse() {
        StrukturertAdresse mottakeradresse = new StrukturertAdresse();
        mottakeradresse.setLandkode("NO");
        mottakeradresse.setGatenavn("gate");
        mottakeradresse.setPostnummer("0463");
        mottakeradresse.setRegion("Oslo");
        mottakeradresse.setHusnummerEtasjeLeilighet("4B");
        mottakeradresse.setPoststed("Oslo");

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost("123456", mottakeradresse, Distribusjonstype.VIKTIG);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        assertThat(captor.getValue().getJournalpostId()).isEqualTo("123456");
        assertThat(captor.getValue().getAdresse().getAdresseType()).isEqualTo("norskPostadresse");
        assertThat(captor.getValue().getDistribusjonstype()).isEqualTo(Distribusjonstype.VIKTIG);
    }

    @Test
    void distribuerJournalpost_utenlandskAdresse() {
        StrukturertAdresse mottakeradresse = new StrukturertAdresse();
        mottakeradresse.setLandkode("SE");
        mottakeradresse.setGatenavn("svensk gate");
        mottakeradresse.setPostnummer("9999");
        mottakeradresse.setRegion("Sverige");
        mottakeradresse.setHusnummerEtasjeLeilighet("4B");
        mottakeradresse.setPoststed("Stockholm");

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost("123456", mottakeradresse, Distribusjonstype.VIKTIG);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        assertThat(captor.getValue().getJournalpostId()).isEqualTo("123456");
        assertThat(captor.getValue().getAdresse().getAdresseType()).isEqualTo("utenlandskPostadresse");
        assertThat(captor.getValue().getDistribusjonstype()).isEqualTo(Distribusjonstype.VIKTIG);
    }

    @Test
    void distribuerJournalpost_utenAdresse() {
        String journalpostId = "123456";

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost(journalpostId, Distribusjonstype.ANNET);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        assertEquals(journalpostId, captor.getValue().getJournalpostId());
        assertNull(captor.getValue().getAdresse());
        assertThat(captor.getValue().getDistribusjonstype()).isEqualTo(Distribusjonstype.ANNET);
    }

    @Test
    void distribuerJournalpost_medStrukturertNorskAdresse_utenKontaktopplysning() {
        String journalpostId = "123456";
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.setGatenavn("Postboks 222");
        strukturertAdresse.setPostnummer("9999");
        strukturertAdresse.setLandkode("NO");

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost(journalpostId, strukturertAdresse, null, null, Distribusjonstype.VEDTAK);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        DistribuerJournalpostRequest request = captor.getValue();
        assertEquals(journalpostId, request.getJournalpostId());
        assertEquals("norskPostadresse", request.getAdresse().getAdresseType());
        assertEquals(strukturertAdresse.getGatenavn(), request.getAdresse().getAdresselinje1());
        assertEquals(strukturertAdresse.getPostnummer(), request.getAdresse().getPostnummer());
        assertThat(captor.getValue().getDistribusjonstype()).isEqualTo(Distribusjonstype.VEDTAK);
    }

    @Test
    void distribuerJournalpost_medStrukturertUtenlandskAdresse_utenKontaktopplysning() {
        String journalpostId = "123456";
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.setGatenavn("Postboks 222");
        strukturertAdresse.setPostnummer("9999");
        strukturertAdresse.setLandkode("BE");

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost(journalpostId, strukturertAdresse, null, null, Distribusjonstype.VEDTAK);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        DistribuerJournalpostRequest request = captor.getValue();
        assertEquals(journalpostId, request.getJournalpostId());
        assertEquals("utenlandskPostadresse", request.getAdresse().getAdresseType());
        assertEquals(strukturertAdresse.getGatenavn(), request.getAdresse().getAdresselinje1());
        assertNull(request.getAdresse().getPostnummer());
        assertThat(captor.getValue().getDistribusjonstype()).isEqualTo(Distribusjonstype.VEDTAK);
    }

    @Test
    void distribuerJournalpost_medStrukturertNorskAdresse_medKontaktopplysning() {
        String journalpostId = "123456";
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.setGatenavn("Postboks 222");
        strukturertAdresse.setPostnummer("9999");
        strukturertAdresse.setLandkode("NO");

        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn("Fetter Anton");

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost(journalpostId, strukturertAdresse, kontaktopplysning, null, Distribusjonstype.VEDTAK);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        DistribuerJournalpostRequest request = captor.getValue();
        assertEquals(journalpostId, request.getJournalpostId());
        assertEquals("norskPostadresse", request.getAdresse().getAdresseType());
        assertEquals("Att: Fetter Anton", request.getAdresse().getAdresselinje1());
        assertEquals(strukturertAdresse.getGatenavn(), request.getAdresse().getAdresselinje2());
        assertEquals(strukturertAdresse.getPostnummer(), request.getAdresse().getPostnummer());
        assertThat(captor.getValue().getDistribusjonstype()).isEqualTo(Distribusjonstype.VEDTAK);
    }

    @Test
    void distribuerJournalpost_medStrukturertNorskAdresse_medKontaktopplysningOgOverstyrtKontaktpersonNavn() {
        String journalpostId = "123456";
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.setGatenavn("Postboks 222");
        strukturertAdresse.setPostnummer("9999");
        strukturertAdresse.setLandkode("NO");

        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn("Fetter Anton");

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost(journalpostId, strukturertAdresse, kontaktopplysning, "Kari Kontakt", Distribusjonstype.ANNET);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        DistribuerJournalpostRequest request = captor.getValue();
        assertEquals(journalpostId, request.getJournalpostId());
        assertEquals("norskPostadresse", request.getAdresse().getAdresseType());
        assertEquals("Att: Kari Kontakt", request.getAdresse().getAdresselinje1());
        assertEquals(strukturertAdresse.getGatenavn(), request.getAdresse().getAdresselinje2());
        assertEquals(strukturertAdresse.getPostnummer(), request.getAdresse().getPostnummer());
        assertThat(captor.getValue().getDistribusjonstype()).isEqualTo(Distribusjonstype.ANNET);
    }

    private Dokumentbestillingsinformasjon hentDokumentBestillingInfoFraCaptor(ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor) {
        return captor.getValue().getDokumentbestillingsinformasjon();
    }

    private DokumentbestillingMetadata lagMetadataForBruker(StrukturertAdresse postadresse) {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.dokumenttypeID = "dok_1234";
        metadata.brukerNavn = "Kim Se";
        metadata.brukerID = FNR;
        metadata.mottaker = lagAktør(Aktoersroller.BRUKER);
        metadata.postadresse = postadresse;
        if (postadresse == null) {
            metadata.berik = true;
        }
        return metadata;
    }

    private Aktoer lagAktør(Aktoersroller rolle) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(rolle);
        switch (rolle) {
            case BRUKER -> aktør.setAktørId(FNR);
            case TRYGDEMYNDIGHET -> aktør.setInstitusjonId(INSITUSJON_ID);
            case REPRESENTANT -> throw new IllegalArgumentException("For representant, bruk lagAktørRepresentant()");
            default -> aktør.setOrgnr(ORGNR);
        }
        return aktør;
    }

    private Aktoer lagAktørRepresentant(Aktoertype mottakerType) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(Aktoersroller.REPRESENTANT);
        switch (mottakerType) {
            case PERSON -> aktør.setPersonIdent(REP_FNR);
            case ORGANISASJON -> aktør.setOrgnr(REP_ORGNR);
            default -> throw new IllegalArgumentException("Mottakertype må være person eller organisasjon");
        }
        return aktør;
    }

    private DokumentbestillingMetadata lagMetadataMedArbeidsgiver() {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.mottaker = lagAktør(Aktoersroller.ARBEIDSGIVER);
        metadata.mottakerID = ORGNR;
        metadata.dokumenttypeID = "dok_1234";
        return metadata;
    }

    private DokumentbestillingMetadata lagMetadataMedRepresentant(Aktoertype representantType) {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.mottaker = lagAktørRepresentant(representantType);
        metadata.mottakerID = switch (representantType) {
            case PERSON -> REP_FNR;
            case ORGANISASJON -> REP_ORGNR;
            default -> throw new IllegalArgumentException("Mottakertype må være person eller organisasjon");
        };
        metadata.dokumenttypeID = "dok_1234";
        return metadata;
    }

    private DokumentbestillingMetadata lagMetadataMedMyndighet() {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.mottaker = lagAktør(Aktoersroller.TRYGDEMYNDIGHET);
        metadata.mottakerID = ORGNR;
        metadata.utenlandskMyndighet = lagUtenlandskMyndighet();
        metadata.dokumenttypeID = "dok_1234";
        return metadata;
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.gateadresse1 = "Stubenstrasse 77";
        utenlandskMyndighet.postnummer = "0101";
        utenlandskMyndighet.poststed = "Berlin";
        utenlandskMyndighet.landkode = Land_iso2.GL;
        utenlandskMyndighet.institusjonskode = "INST-023%zdf";
        return utenlandskMyndighet;
    }

    private static Element lagBrevData() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
        Document doc = builder.newDocument();

        return doc.createElement("brevData");
    }
}
