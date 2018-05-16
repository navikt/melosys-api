package no.nav.melosys.service.dokumentmottak;

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

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class DokumentmottakConsumerImpl {

    private static final Logger log = LoggerFactory.getLogger(DokumentmottakConsumerImpl.class);

    private final JAXBContext jaxbContext;

    private final Meldingsfordeler meldingsfordeler;

    @Autowired
    public DokumentmottakConsumerImpl(Meldingsfordeler meldingsfordeler) {
        try {
            jaxbContext = JAXBContext.newInstance(ForsendelsesinformasjonDto.class);
        } catch (JAXBException e) {
            log.error("", e);
            throw new IntegrasjonException(e);
        }
        this.meldingsfordeler = meldingsfordeler;
    }

    @JmsListener(destination = "${dokumentmottak.queueName}")
    public void mottaDokument(Message message) throws JMSException {
        if (message instanceof TextMessage) {
            String xml = ((TextMessage) message).getText();
            ForsendelsesinformasjonDto forsendelsesinfo = parseXml(xml);
            meldingsfordeler.execute(forsendelsesinfo);
        } else {
            log.error("Kunne ikke lese melding fra dokumentmottak");
            throw new IntegrasjonException("Kunne ikke lese melding fra dokumentmottak");
        }
    }

    private ForsendelsesinformasjonDto parseXml(String xml) throws JMSException {
        Source source = new StreamSource(new StringReader(xml));
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<ForsendelsesinformasjonDto> element = unmarshaller.unmarshal(source, ForsendelsesinformasjonDto.class);
            log.info("Mottatt melding fra dokumentmottak", element.getValue());
            return element.getValue();
        } catch (JAXBException e) {
            log.error("Kunne ikke lese melding fra dokumentmottak", e);
            throw new IntegrasjonException("Kunne ikke lese melding fra dokumentmottak", e);
        }
    }

}
