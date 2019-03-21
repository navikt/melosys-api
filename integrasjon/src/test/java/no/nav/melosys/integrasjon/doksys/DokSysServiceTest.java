package no.nav.melosys.integrasjon.doksys;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.Adresse;
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

    private DoksysService dokSysService;

    @Before
    public void setUp() {
        dokSysService = new DoksysService(dokumentproduksjonConsumer);
    }

    @Test
    public void produserDokumentutkast() throws IntegrasjonException, ProduserDokumentutkastBrevdataValideringFeilet, ProduserDokumentutkastInputValideringFeilet {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.dokumenttypeID = "dok_1234";
        when(dokumentproduksjonConsumer.produserDokumentutkast(any())).thenReturn(new ProduserDokumentutkastResponse());

        dokSysService.produserDokumentutkast(metadata, lagBrevData());

        ArgumentCaptor<ProduserDokumentutkastRequest> captor = ArgumentCaptor.forClass(ProduserDokumentutkastRequest.class);
        verify(dokumentproduksjonConsumer).produserDokumentutkast(captor.capture());
        ProduserDokumentutkastRequest dokumentutkastRequest = captor.getValue();
        assertThat(dokumentutkastRequest.getDokumenttypeId()).isEqualTo(metadata.dokumenttypeID);
    }

    @Test
    public void produserIkkeredigerbartDokument() throws ProduserIkkeredigerbartDokumentDokumentErRedigerbart, ProduserIkkeRedigerbartDokumentJoarkForretningsmessigUnntak,
        ProduserIkkeredigerbartDokumentSikkerhetsbegrensning, ProduserIkkeredigerbartDokumentBrevdataValideringFeilet, ProduserIkkeredigerbartDokumentDokumentErVedlegg,
        ProduserIkkeRedigerbartDokumentInputValideringFeilet, FunksjonellException, TekniskException {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.dokumenttypeID = "dok_1234";
        metadata.mottakersRolle = Aktoersroller.BRUKER;
        metadata.utledRegisterInfo = true;
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(metadata, lagBrevData());

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());
        ProduserIkkeredigerbartDokumentRequest dokumentRequest = captor.getValue();
        assertThat(dokumentRequest.getDokumentbestillingsinformasjon().getDokumenttypeId()).isEqualTo(metadata.dokumenttypeID);
    }

    @Test
    public void produserIkkeredigerbartDokument_tilUtenlandskMyndighet() throws ProduserIkkeredigerbartDokumentDokumentErRedigerbart, ProduserIkkeRedigerbartDokumentJoarkForretningsmessigUnntak,
        ProduserIkkeredigerbartDokumentSikkerhetsbegrensning, ProduserIkkeredigerbartDokumentBrevdataValideringFeilet, ProduserIkkeredigerbartDokumentDokumentErVedlegg,
        ProduserIkkeRedigerbartDokumentInputValideringFeilet, FunksjonellException, TekniskException {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.mottakersRolle = Aktoersroller.MYNDIGHET;
        metadata.utenlandskMyndighet = new UtenlandskMyndighet();
        metadata.utenlandskMyndighet.gateadresse = "Stubenstrasse 77";
        metadata.utenlandskMyndighet.poststed = "0101";
        metadata.utenlandskMyndighet.landkode = Landkoder.GL;
        metadata.utenlandskMyndighet.institusjonskode = "INST-023%zdf";
        metadata.dokumenttypeID = "dok_1234";
        when(dokumentproduksjonConsumer.produserIkkeredigerbartDokument(any())).thenReturn(new ProduserIkkeredigerbartDokumentResponse());

        dokSysService.produserIkkeredigerbartDokument(metadata, lagBrevData());

        ArgumentCaptor<ProduserIkkeredigerbartDokumentRequest> captor = ArgumentCaptor.forClass(ProduserIkkeredigerbartDokumentRequest.class);
        verify(dokumentproduksjonConsumer).produserIkkeredigerbartDokument(captor.capture());
        ProduserIkkeredigerbartDokumentRequest dokumentRequest = captor.getValue();
        assertThat(dokumentRequest.getDokumentbestillingsinformasjon().getMottaker()).isInstanceOf(Person.class);
        Adresse adresse = dokumentRequest.getDokumentbestillingsinformasjon().getAdresse();
        assertThat(adresse).isInstanceOf(UtenlandskPostadresse.class);
        UtenlandskPostadresse utenlandskPostadresse = (UtenlandskPostadresse) adresse;
        assertThat(utenlandskPostadresse.getAdresselinje1()).isEqualTo(metadata.utenlandskMyndighet.gateadresse);
        assertThat(utenlandskPostadresse.getLand().getValue()).isEqualTo(metadata.utenlandskMyndighet.landkode.getKode());

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