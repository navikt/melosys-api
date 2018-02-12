package no.nav.melosys.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.repository.BehandlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Service som kaller regelmodulen.
 */
@Service
public class RegelmodulService {

    private final String APPLICATION_JSON_UTF_8 = "application/json;charset=utf-8";

    private String regelmodulUrl;

    private BehandlingRepository behandlingRepo;

    DocumentBuilder documentBuilder;

    private Transformer transformer;

    private static Logger log = LoggerFactory.getLogger(RegelmodulService.class);

    @Autowired
    public RegelmodulService(@Value("${melosys.service.regelmodul.url}") String regelmodulUrl, BehandlingRepository repository) {
        this.regelmodulUrl = regelmodulUrl;
        this.behandlingRepo = repository;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (TransformerConfigurationException | ParserConfigurationException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Kall til regelmodulen med opplysninger knyttet til behandlingen med ID {@code behandlingID}.
     * @param behandlingID Database ID til den behandlingen som brukes for å konstruere requesten til regelmodulen.
     */
    public FastsettLovvalgReply fastsettLovvalg(long behandlingID) {
        Behandling behandling = behandlingRepo.findOne(behandlingID);
        if (behandling == null) {
            // Ikke funnet
            return null;
        }

        try {
            String APPLICATION_JSON_UTF_8 = "application/json;charset=utf-8";
            String APPLICATION_XML_UTF_8 = "application/xml;charset=utf-8";

            String fastsettLovvalgRequest = lagNyRequest(behandling);

            return ClientBuilder.newClient().target(regelmodulUrl)
                    .request(APPLICATION_JSON_UTF_8)
                    .post(Entity.entity(fastsettLovvalgRequest, APPLICATION_XML_UTF_8), FastsettLovvalgReply.class);

        } catch (RuntimeException e) {
            log.error("", e);
            return null;
        }
    }

    /**
     * Lager en request til regelmodulen for en gitt behandling.
     */
    private String lagNyRequest(Behandling behandling) {
        // FIXME: Bedre feilhåndtering + rydd kode
        Document document = documentBuilder.newDocument();

        Element dokumenter = document.createElement("dokumenter");
        Element søknadDokument = document.createElement("søknadDokument");
        Element personopplysningDokument = document.createElement("personopplysningDokument");
        Element arbeidsforholdDokumenter = document.createElement("arbeidsforholdDokumenter");
        Element inntektDokumenter = document.createElement("inntektDokumenter");
        Element medlemskapDokumenter = document.createElement("medlemskapDokumenter");
        Element organisasjonDokumenter = document.createElement("organisasjonDokumenter");

        for (Saksopplysning saksopplysning : behandling.getSaksopplysninger()) {
            SaksopplysningType type = saksopplysning.getType();
            Element dokumentnode = xmlTilNode(saksopplysning.getInternXml(), documentBuilder, document);

            switch (type) {
                case ARBEIDSFORHOLD:
                    arbeidsforholdDokumenter.appendChild(dokumentnode);
                    break;
                case INNTEKT:
                    inntektDokumenter.appendChild(dokumentnode);
                    break;
                case MEDLEMSKAP:
                    medlemskapDokumenter.appendChild(dokumentnode);
                    break;
                case ORGANISASJON:
                    organisasjonDokumenter.appendChild(dokumentnode);
                    break;
                case PERSONOPPLYSNING:
                    personopplysningDokument.appendChild(dokumentnode);
                    break;
                case SØKNAD:
                    søknadDokument.appendChild(dokumentnode);
                    break;
                default:
                    throw new IllegalArgumentException("Type " + type.getKode() + " ikke støttet.");
            }
        }

        if (søknadDokument.hasChildNodes()) {
            dokumenter.appendChild(søknadDokument);
        }
        if (personopplysningDokument.hasChildNodes()) {
            dokumenter.appendChild(personopplysningDokument);
        }
        if (arbeidsforholdDokumenter.hasChildNodes()) {
            dokumenter.appendChild(arbeidsforholdDokumenter);
        }
        if (inntektDokumenter.hasChildNodes()) {
            dokumenter.appendChild(inntektDokumenter);
        }
        if (medlemskapDokumenter.hasChildNodes()) {
            dokumenter.appendChild(medlemskapDokumenter);
        }
        if (organisasjonDokumenter.hasChildNodes()) {
            dokumenter.appendChild(organisasjonDokumenter);
        }

        document.appendChild(dokumenter);

        DOMSource source = new DOMSource(document);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(outputStream);

        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

        return outputStream.toString();
    }

    private Element xmlTilNode(String xml, DocumentBuilder builder, Document document) {
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        try {
            Element node = builder.parse(inputStream).getDocumentElement();
            document.adoptNode(node);
            return node;
        } catch (SAXException | IOException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }
}
