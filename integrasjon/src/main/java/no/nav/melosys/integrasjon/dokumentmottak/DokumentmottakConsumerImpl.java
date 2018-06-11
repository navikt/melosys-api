package no.nav.melosys.integrasjon.dokumentmottak;

import java.io.StringReader;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import no.nav.melosys.exception.IntegrasjonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import static no.nav.melosys.integrasjon.felles.jms.JmsConfig.DOKUMENTKØ;

@Profile("!mocking") //FIXME MELOSYS-1284
@Component
public class DokumentmottakConsumerImpl {

    private static final Logger log = LoggerFactory.getLogger(DokumentmottakConsumerImpl.class);

    private final JAXBContext jaxbContext;

    private final ProsessinstansMeldingsfordeler meldingsfordeler;

    @Autowired
    public DokumentmottakConsumerImpl(ProsessinstansMeldingsfordeler meldingsfordeler) {
        try {
            jaxbContext = JAXBContext.newInstance(ForsendelsesinformasjonDto.class);
        } catch (JAXBException e) {
            log.error("Feilet ved instansiering av konsument", e);
            throw new IntegrasjonException(e);
        }
        this.meldingsfordeler = meldingsfordeler;
    }

    @JmsListener(destination = DOKUMENTKØ)
    public void mottaDokument(Message message) {
        if (message instanceof TextMessage) {
            try {
                String xml = ((TextMessage) message).getText();
                ForsendelsesinformasjonDto forsendelsesinfo = parseXml(xml);
                meldingsfordeler.execute(forsendelsesinfo);
            } catch (JAXBException e) {
                log.error("Klarte ikke å tolke mottatt melding", e);
            } catch (JMSException | RuntimeException e) {
                log.error("Klarte ikke å opprette prosessinstans", e);
            }
        } else {
            log.error("Klarte ikke å lese melding fra dokumentmottak");
        }
    }

    private ForsendelsesinformasjonDto parseXml(String xml) throws JAXBException, JMSException {
        Source source = new StreamSource(new StringReader(xml));
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<ForsendelsesinformasjonDto> element = unmarshaller.unmarshal(source, ForsendelsesinformasjonDto.class);
        log.info("Mottatt melding fra dokumentmottak", element.getValue());
        return element.getValue();
    }

}
