package no.nav.melosys.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.regler.api.lovvalg.LovvalgTjeneste;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.rep.VurderInngangsvilkaarReply;
import no.nav.melosys.repository.BehandlingRepository;

/**
 * Service som kaller regelmodulen.
 */
@Service
public class RegelmodulService {

    private String regelmodulUrl;

    private BehandlingRepository behandlingRepo;

    private DocumentBuilderFactory documentBuilderFactory;

    private TransformerFactory transformerFactory;

    private static Logger log = LoggerFactory.getLogger(RegelmodulService.class);

    @Autowired
    public RegelmodulService(@Value("${melosys.service.regelmodul.url}") String regelmodulUrl, BehandlingRepository repository) {
        this.regelmodulUrl = regelmodulUrl;
        this.behandlingRepo = repository;
        this.transformerFactory = TransformerFactory.newInstance();
        this.documentBuilderFactory = DocumentBuilderFactory.newInstance();
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
            String fastsettLovvalgRequest = lagRequest(behandling);

            return ClientBuilder.newClient().target(regelmodulUrl)
                    .request(LovvalgTjeneste.MEDIA_TYPE_CONSUMED)
                    .post(Entity.entity(fastsettLovvalgRequest, LovvalgTjeneste.MEDIA_TYPE_CONSUMED), FastsettLovvalgReply.class);

        } catch (ParserConfigurationException | TransformerException | IOException | SAXException e) {
            log.error("Uventet feil ved generering av inndata til Regelmodul", e);
        }
        return null;
    }

    /**
     * Lager en request til regelmodulen for en gitt behandling.
     */
    private String lagRequest(Behandling behandling) throws ParserConfigurationException, TransformerException, IOException, SAXException {
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
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

        transformerFactory.newTransformer().transform(source, result);
        return outputStream.toString();
    }

    private Element xmlTilNode(String xml, DocumentBuilder builder, Document document) throws IOException, SAXException {
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        Element node = builder.parse(inputStream).getDocumentElement();
        document.adoptNode(node);
        return node;
    }
    
    /**
     * Kaller regelmodulen for å kjøre inngangsvilkårsvurdering
     * 
     * @throws ProcessingException Hvis request- eller reply-prosessering feiler, eller hvis IO-feil ved kommunikasjon med regelmodulen
     * @throws WebApplicationException Hvis regelmodulen returnerer noe annet enn HTTP 2xx
     */
    public VurderInngangsvilkaarReply vurderInngangsvilkår(Land brukersStatsborgerskap, List<Land> oppholdsland, Periode oppholdsPeriode) {
        Assert.notNull(brukersStatsborgerskap, "Tjenesten krever at brukersStatsborgerskap ikke er null");
        Assert.notEmpty(oppholdsland, "Tjenesten krever at oppholdsland ikke er null eller tom");
        Assert.notNull(oppholdsPeriode, "Tjenesten krever at oppholdsPeriode ikke er null");
        Assert.notNull(oppholdsPeriode.getFom(), "Tjenesten krever at oppholdsPeriode har fom dato");
        
        // FIXME: Nille-variant i påvente av hva som skjer med regelmodulen.
        String format = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
            "<dokumenter><personDokument xmlns:tps3=\"http://nav.no/tjeneste/virksomhet/person/v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "<statsborgerskap><kode>%s</kode></statsborgerskap></personDokument><soeknadDokument><oppholdUtland><oppholdsPeriode><fom>%s</fom><tom>" +
            "%s</tom></oppholdsPeriode>" +
            "<oppholdsland><kode>%s</kode></oppholdsland></oppholdUtland></soeknadDokument></dokumenter>";
        String req = String.format(format, brukersStatsborgerskap, oppholdsPeriode.getFom(), oppholdsPeriode.getTom(), oppholdsland);
        
        return ClientBuilder.newClient().target(regelmodulUrl)
            .request(LovvalgTjeneste.MEDIA_TYPE_CONSUMED)
            .post(Entity.entity(req, LovvalgTjeneste.MEDIA_TYPE_CONSUMED), VurderInngangsvilkaarReply.class);
    }

}
