package no.nav.melosys.integrasjon.medl2.medlemskap;

import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

public class MedlemskapMock implements MedlemskapConsumer {

    @Override
    public HentPeriodeListeResponse hentPeriodeListe(HentPeriodeListeRequest request) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        String ident = request.getIdent().getValue();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(HentPeriodeListeResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputStream is = getClass().getClassLoader().getResourceAsStream("mock/medlemskap/" + ident + ".xml");

            if (is == null) {
                throw new PersonIkkeFunnet("Person med ident " + ident + "ikke funnet.", null);
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document document = factory.newDocumentBuilder().parse(is);
            NodeList nodeList = document.getElementsByTagName("periodeListe");

            HentPeriodeListeResponse response = new HentPeriodeListeResponse();

            for (int i = 0; i < nodeList.getLength(); i++) {
                JAXBElement<Medlemsperiode> element = unmarshaller.unmarshal(nodeList.item(i), Medlemsperiode.class);
                response.getPeriodeListe().add(element.getValue());
            }

            return response;
        } catch (JAXBException | ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
