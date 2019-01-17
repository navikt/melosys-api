package no.nav.melosys.integrasjon.medl.medlemskap;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MedlemskapMock implements MedlemskapConsumer {

    @Override
    public HentPeriodeListeResponse hentPeriodeListe(HentPeriodeListeRequest request) throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        String ident = request.getIdent().getValue();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mock/medlemskap/" + ident + ".xml")) {
            JAXBContext jaxbContext = JAXBContext.newInstance(HentPeriodeListeResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            if (is == null) {
                throw new PersonIkkeFunnet("Person med ident " + ident + " ikke funnet.", null);
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
