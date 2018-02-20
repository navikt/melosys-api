package no.nav.melosys.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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

    private String regelmodulUrl;

    private BehandlingRepository behandlingRepo;

    private DocumentBuilder documentBuilder;

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
            log.error("Uventet feil ved oppretting av RegelmodulService", e);
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
            String APPLICATION_XML_UTF_8 = "application/xml;charset=utf-8";

            String fastsettLovvalgRequest = lagRequest(behandling);

            return ClientBuilder.newClient().target(regelmodulUrl)
                    .request(APPLICATION_XML_UTF_8)
                    .post(Entity.entity(fastsettLovvalgRequest, APPLICATION_XML_UTF_8), FastsettLovvalgReply.class);

        } catch (RuntimeException e) {
            log.error("Uventet feil ved generering av inndata til Regelmodul", e);
        }
        return null;
    }

    /**
     * Lager en request til regelmodulen for en gitt behandling.
     */
    private String lagRequest(Behandling behandling) {
        // FIXME: Bedre feilhåndtering + rydd kode
        Document document = documentBuilder.newDocument();

        Element dokumenter = document.createElement("dokumenter");
        Map<String, Element> dokumentnoder = new HashMap<>();
        dokumentnoder.put("arbeidsforholdDokumenter", document.createElement("arbeidsforholdDokumenter"));
        dokumentnoder.put("inntektDokumenter", document.createElement("inntektDokumenter"));
        dokumentnoder.put("medlemskapDokumenter", document.createElement("medlemskapDokumenter"));
        dokumentnoder.put("organisasjonDokumenter", document.createElement("organisasjonDokumenter"));

        for (Saksopplysning saksopplysning : behandling.getSaksopplysninger()) {
            SaksopplysningType type = saksopplysning.getType();
            Element dokumentnode = xmlTilNode(saksopplysning.getInternXml(), documentBuilder, document);

            switch (type) {
                case ARBEIDSFORHOLD:
                    dokumentnoder.get("arbeidsforholdDokumenter").appendChild(dokumentnode);
                    break;
                case INNTEKT:
                    dokumentnoder.get("inntektDokumenter").appendChild(dokumentnode);
                    break;
                case MEDLEMSKAP:
                    dokumentnoder.get("medlemskapDokumenter").appendChild(dokumentnode);
                    break;
                case ORGANISASJON:
                    dokumentnoder.get("organisasjonDokumenter").appendChild(dokumentnode);
                    break;
                case PERSONOPPLYSNING:
                    dokumentnoder.put("personDokument", dokumentnode);
                    break;
                case SØKNAD:
                    dokumentnoder.put("soeknadDokument", dokumentnode);
                    break;
                default:
                    throw new IllegalArgumentException("Type " + type.getKode() + " ikke støttet.");
            }
        }

        for (Element dokumentnode : dokumentnoder.values()) {
            if (dokumentnode.hasChildNodes()) {
                dokumenter.appendChild(dokumentnode);
            }
        }

        if (dokumenter.hasChildNodes()) {
            document.appendChild(dokumenter);
        }

        DOMSource source = new DOMSource(document);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(outputStream);

        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
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
            throw new RuntimeException(e);
        }
    }
}
