package no.nav.melosys.integrasjon.sakogbehandling;

import java.io.StringWriter;
import javax.jms.Message;
import javax.jms.Queue;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingStatus;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.ObjectFactory;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

@Component
public class SakOgBehandlingClientImpl implements SakOgBehandlingClient {

    private static final Logger log = LoggerFactory.getLogger(SakOgBehandlingClientImpl.class);

    private final JAXBContext jaxbContext;

    private final ObjectFactory objectFactory;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Queue hendelseshåndterer;

    public SakOgBehandlingClientImpl() {
        try {
            jaxbContext = JAXBContext.newInstance(BehandlingStatus.class);
        } catch (JAXBException e) {
            log.error("Feilet ved instansiering av konsument", e);
            throw new IntegrasjonException(e);
        }
        this.objectFactory = new ObjectFactory();
    }

    @Override
    public void sendBehandlingOpprettet(BehandlingOpprettet behandlingOpprettet) {
        jmsTemplate.convertAndSend(hendelseshåndterer, behandlingStatusTilXml(behandlingOpprettet), behandleMelding);
    }

    @Override
    public void sendBehandlingAvsluttet(BehandlingAvsluttet behandlingAvsluttet) {
        jmsTemplate.convertAndSend(hendelseshåndterer, behandlingStatusTilXml(behandlingAvsluttet), behandleMelding);
    }

    public String behandlingStatusTilXml(BehandlingStatus behandlingStatus) {
        try {
            JAXBElement xmlElement = null;
            if (behandlingStatus instanceof BehandlingOpprettet) {
                xmlElement = objectFactory.createBehandlingOpprettet((BehandlingOpprettet) behandlingStatus);
            } else if (behandlingStatus instanceof BehandlingAvsluttet) {
                xmlElement = objectFactory.createBehandlingAvsluttet((BehandlingAvsluttet) behandlingStatus);
            }
            Marshaller marshaller = jaxbContext.createMarshaller();
            StringWriter writer = new StringWriter();
            marshaller.marshal(xmlElement, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }
    }

    private MessagePostProcessor behandleMelding = (Message message) -> {
        message.setStringProperty("callId", MDCOperations.getFromMDC(MDCOperations.MDC_CALL_ID));
        return message;
    };

}
