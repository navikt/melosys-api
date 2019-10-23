package no.nav.melosys.integrasjon.doksys;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.DistribuerJournalpostConsumer;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse;
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.UtenlandskPostadresse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DokSysServiceTest {

    @Mock
    private DokumentproduksjonConsumer dokumentproduksjonConsumer;
    @Mock
    private DistribuerJournalpostConsumer distribuerJournalpostConsumer;

    private DoksysService dokSysService;

    @Before
    public void setUp() {
        dokSysService = new DoksysService(dokumentproduksjonConsumer, distribuerJournalpostConsumer);
    }

    @Test
    public void produserDokumentutkast() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataForBruker(null);
        when(dokumentproduksjonConsumer.produserDokumentutkast(any())).thenReturn(new ProduserDokumentutkastResponse());

        dokSysService.produserDokumentutkast(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserDokumentutkastRequest> captor = ArgumentCaptor.forClass(ProduserDokumentutkastRequest.class);
        verify(dokumentproduksjonConsumer).produserDokumentutkast(captor.capture());
        ProduserDokumentutkastRequest dokumentutkastRequest = captor.getValue();
        assertThat(dokumentutkastRequest.getDokumenttypeId()).isEqualTo(metadata.dokumenttypeID);
    }

    @Test
    public void produserIkkeredigerbartDokument_forBruker() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataForBruker(null);
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());
        ProduserIkkeredigerbartDokumentRequest dokumentRequest = captor.getValue();
        assertThat(dokumentRequest.getDokumentbestillingsinformasjon().getDokumenttypeId()).isEqualTo(metadata.dokumenttypeID);
        assertThat(dokumentRequest.getDokumentbestillingsinformasjon().getAdresse()).isNull();
    }

    @Test
    public void produserIkkeredigerbartDokument_forBrukerMedPostadresse_girPostadresseOgNavn() throws Exception {
        StrukturertAdresse postadresse = new StrukturertAdresse();
        postadresse.gatenavn = "Gatenavn";
        postadresse.husnummer = "123";
        postadresse.postnummer = "1337";
        postadresse.poststed = "Poststed";
        postadresse.region = "Region";
        postadresse.landkode = "BE";
        DokumentbestillingMetadata metadata = lagMetadataForBruker(postadresse);
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());
        UtenlandskPostadresse adresse = hentAdresseFraCaptor(captor);
        assertThat(adresse.getAdresselinje1()).isEqualTo(postadresse.gatenavn+" "+postadresse.husnummer);
        assertThat(adresse.getAdresselinje2()).isEqualTo(postadresse.postnummer+" "+postadresse.poststed);
        assertThat(adresse.getAdresselinje3()).isEqualTo(postadresse.region);
        assertThat(adresse.getLand().getValue()).isEqualTo(postadresse.landkode);
        assertThat(((Person)captor.getValue().getDokumentbestillingsinformasjon().getBruker()).getNavn()).isEqualTo("Kim Se");
    }

    private UtenlandskPostadresse hentAdresseFraCaptor(ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor) {
        return (UtenlandskPostadresse) captor.getValue().getDokumentbestillingsinformasjon().getAdresse();
    }

    private DokumentbestillingMetadata lagMetadataForBruker(StrukturertAdresse postadresse) {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.dokumenttypeID = "dok_1234";
        metadata.brukerNavn = "Kim Se";
        metadata.mottaker = lagAktør(Aktoersroller.BRUKER);
        metadata.postadresse = postadresse;
        return metadata;
    }

    private Aktoer lagAktør(Aktoersroller rolle) {
        Aktoer aktør = new Aktoer();
        aktør.setRolle(rolle);
        if (rolle == Aktoersroller.BRUKER) {
            aktør.setAktørId("1234");
        } else if (rolle == Aktoersroller.MYNDIGHET) {
            aktør.setInstitusjonId("DK:234");
        } else {
            aktør.setOrgnr("98765");
        }
        return aktør;
    }

    @Test
    public void produserIkkeredigerbartDokument_tilUtenlandskMyndighet() throws Exception {
        DokumentbestillingMetadata metadata = lagMetadataMedMyndighet();
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(new Dokumentbestilling(metadata, lagBrevData()));

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());

        ProduserIkkeredigerbartDokumentRequest dokumentRequest = captor.getValue();
        assertThat(dokumentRequest.getDokumentbestillingsinformasjon().getMottaker()).isInstanceOf(Person.class);

        UtenlandskPostadresse utenlandskPostadresse = hentAdresseFraCaptor(captor);
        assertThat(utenlandskPostadresse.getAdresselinje1()).isEqualTo(metadata.utenlandskMyndighet.gateadresse);
        assertThat(utenlandskPostadresse.getLand().getValue()).isEqualTo(metadata.utenlandskMyndighet.landkode.getKode());
    }

    private DokumentbestillingMetadata lagMetadataMedMyndighet() {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.mottaker = lagAktør(Aktoersroller.MYNDIGHET);
        metadata.utenlandskMyndighet = lagUtenlandskMyndighet();
        metadata.dokumenttypeID = "dok_1234";
        return metadata;
    }

    private UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.gateadresse = "Stubenstrasse 77";
        utenlandskMyndighet.poststed = "0101";
        utenlandskMyndighet.landkode = Landkoder.GL;
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

    @Test
    public void distribuerJournalpost_norskAdresse() {
        StrukturertAdresse mottakeradresse = new StrukturertAdresse();
        mottakeradresse.landkode = "NO";
        mottakeradresse.gatenavn = "gate";
        mottakeradresse.postnummer = "0463";
        mottakeradresse.region = "Oslo";
        mottakeradresse.husnummer = "4B";
        mottakeradresse.poststed = "Oslo";

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost("123456", mottakeradresse);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        assertThat(captor.getValue().getJournalpostId()).isEqualTo("123456");
        assertThat(captor.getValue().getAdresse().getAdressetype()).isEqualTo("norskPostadresse");
    }

    @Test
    public void distribuerJournalpost_utenlandskAdresse() {
        StrukturertAdresse mottakeradresse = new StrukturertAdresse();
        mottakeradresse.landkode = "SE";
        mottakeradresse.gatenavn = "svensk gate";
        mottakeradresse.postnummer = "9999";
        mottakeradresse.region = "Sverige";
        mottakeradresse.husnummer = "4B";
        mottakeradresse.poststed = "Stockholm";

        when(distribuerJournalpostConsumer.distribuerJournalpost(any(DistribuerJournalpostRequest.class)))
            .thenReturn(new DistribuerJournalpostResponse("123"));

        dokSysService.distribuerJournalpost("123456", mottakeradresse);

        ArgumentCaptor<DistribuerJournalpostRequest> captor = ArgumentCaptor.forClass(DistribuerJournalpostRequest.class);
        verify(distribuerJournalpostConsumer).distribuerJournalpost(captor.capture());

        assertThat(captor.getValue().getJournalpostId()).isEqualTo("123456");
        assertThat(captor.getValue().getAdresse().getAdressetype()).isEqualTo("utenlandskPostadresse");
    }
}