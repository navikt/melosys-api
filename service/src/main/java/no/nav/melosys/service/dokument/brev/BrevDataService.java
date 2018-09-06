package no.nav.melosys.service.dokument.brev;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.foreldrepenger.integrasjon.dokument.felles.*;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.DokumentType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * BrevDataService er ansvarlig for å mappe saksopplysninger i brevmalene.
 */
@Service
public class BrevDataService {

    private TpsFasade tpsFasade;

    @Autowired
    public BrevDataService(TpsFasade tpsFasade) {
        this.tpsFasade = tpsFasade;
    }

    /**
     * Genererer metada til doksys angående dokumentbestillingen.
     */
    public DokumentbestillingMetadata lagBestillingMetadata(DokumentType dokumentType, Behandling behandling) {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();

        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.getBruker();
        if (bruker != null) {
            try {
                metadata.bruker = tpsFasade.hentIdentForAktørId(bruker.getAktørId());
            } catch (IkkeFunnetException e) {
                throw new TekniskException("Det finnes ingen ident for aktørID " + bruker.getAktørId());
            }
        } else {
            throw new TekniskException("Det finnes ingen bruker på sak " + fagsak.getSaksnummer());
        }

        metadata.dokumenttypeID = dokumentType.getKode();
        metadata.journalsakID = Integer.toString(fagsak.getGsakSaksnummer());
        // FIXME Mottaker er avhengig av dokumentTypen men kan også sendes som parameter
        metadata.mottaker = null;
        // FIXME Fagområde er avhengig av dokumentTypen men kan også sendes som parameter.
        metadata.fagområde = null;

        return metadata;
    }

    /**
     * Genererer XML i hensyn til mal og validere mot xsd.
     */
    public Element lagBrevXML(DokumentType dokumentType, Behandling behandling) {
        Element brevXmlElement;
        try {
            FellesType fellesType = mapFellesType(behandling);
            String brevXml = BrevDataMapperRuter.brevDataMapper(dokumentType).mapTilBrevXML(fellesType, behandling);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(brevXml));
            Document doc = db.parse(is);
            brevXmlElement = doc.getDocumentElement();
        } catch (JAXBException | SAXException | ParserConfigurationException | IOException e) {
            throw new TekniskException("XML genereringsfeil " + behandling.getFagsak().getSaksnummer(), e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TekniskException("Annen teknisk feil", e);
        }
        return brevXmlElement;
    }

    // FIXME Venter på riktig XSD
    public FellesType mapFellesType(Behandling behandling) {
        final FellesType fellesType = new FellesType();
        fellesType.setSpraakkode(SpraakkodeType.fromValue(SpraakkodeType.NB.value()));
        fellesType.setFagsaksnummer(behandling.getFagsak().getSaksnummer());

        String userID = SubjectHandler.getInstance().getUserID();
        if (userID != null) {
            SignerendeSaksbehandlerType signerendeSaksbehandlerType = new SignerendeSaksbehandlerType();
            signerendeSaksbehandlerType.setSignerendeSaksbehandlerNavn(userID);
            fellesType.setSignerendeSaksbehandler(signerendeSaksbehandlerType);
        }
        fellesType.setAutomatiskBehandlet(false);
        SakspartType sakspartType = new SakspartType();
        sakspartType.setSakspartId("MEL-001");
        sakspartType.setSakspartTypeKode(IdKodeType.PERSON);
        sakspartType.setSakspartNavn("Test");
        fellesType.setSakspart(sakspartType);

        fellesType.setMottaker(lageMottakerType(behandling));
        fellesType.setNavnAvsenderEnhet("NavnAvsenderEnhet");
        fellesType.setNummerAvsenderEnhet("NummerAvsenderEnhet");
        fellesType.setKontaktInformasjon(BrevDataUtils.lageKontaktInformasjonType(behandling));

        try {
            fellesType.setDokumentDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil", e);
        }

        return fellesType;
    }

    private MottakerType lageMottakerType(Behandling behandling) {
        MottakerType mottakerType = new MottakerType();
        mottakerType.setMottakerId("Mottaker-ID");
        mottakerType.setMottakerTypeKode(IdKodeType.PERSON);
        mottakerType.setMottakerNavn("Mottaker-Navn");
        MottakerAdresseType mottakerAdresseType = new MottakerAdresseType();

        mottakerAdresseType.setAdresselinje1("Linje_1");
        mottakerAdresseType.setAdresselinje2("Linje_2");
        mottakerAdresseType.setAdresselinje3("Linje_3");
        mottakerAdresseType.setPostNr("7777");
        mottakerAdresseType.setPoststed("Poststed");
        mottakerAdresseType.setLand("NO");
        mottakerType.setMottakerAdresse(mottakerAdresseType);
        return mottakerType;
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendarRemoveTimezone(LocalDate localDate) throws DatatypeConfigurationException {
        if (localDate == null) {
            return null;
        }
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(
            localDate.getYear(),
            localDate.getMonthValue(),
            localDate.getDayOfMonth(),
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED,
            DatatypeConstants.FIELD_UNDEFINED
        );
    }
}
