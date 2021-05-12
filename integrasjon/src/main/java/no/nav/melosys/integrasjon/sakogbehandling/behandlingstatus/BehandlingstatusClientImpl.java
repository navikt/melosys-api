package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import java.io.StringWriter;
import javax.jms.Message;
import javax.jms.Queue;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingStatus;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.ObjectFactory;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import static no.nav.melosys.integrasjon.felles.jms.JmsConfig.HENDELSESKØ;

@Component
public class BehandlingstatusClientImpl implements BehandlingstatusClient {
    private static final long DEFAULT_TIMEOUT = 5000;

    private final JmsTemplate jmsTemplate;
    private final Queue hendelseshåndterer;
    private final Jaxb2Marshaller jaxb2Marshaller;
    private final ObjectFactory objectFactory;

    @Autowired
    public BehandlingstatusClientImpl(JmsTemplate jmsTemplate,
                                      @Qualifier(HENDELSESKØ) Queue hendelseshåndterer,
                                      Jaxb2Marshaller jaxb2Marshaller) {
        this.jmsTemplate = jmsTemplate;
        this.hendelseshåndterer = hendelseshåndterer;
        this.jaxb2Marshaller = jaxb2Marshaller;
        this.objectFactory = new ObjectFactory();
        this.jmsTemplate.setReceiveTimeout(DEFAULT_TIMEOUT);
    }

    @Override
    public void sendBehandlingOpprettet(BehandlingStatusMapper mapper) {
        BehandlingOpprettet behandlingOpprettet = mapper.tilBehandlingOpprettet();
        jmsTemplate.convertAndSend(hendelseshåndterer, behandlingStatusTilXml(behandlingOpprettet), behandleMelding);
    }

    @Override
    public void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) {
        BehandlingAvsluttet behandlingAvsluttet = mapper.tilBehandlingAvsluttet();
        jmsTemplate.convertAndSend(hendelseshåndterer, behandlingStatusTilXml(behandlingAvsluttet), behandleMelding);
    }

    private String behandlingStatusTilXml(BehandlingStatus behandlingStatus) {
        try {
            JAXBElement<?> xmlElement;
            if (behandlingStatus instanceof BehandlingOpprettet) {
                xmlElement = objectFactory.createBehandlingOpprettet((BehandlingOpprettet) behandlingStatus);
            } else if (behandlingStatus instanceof BehandlingAvsluttet) {
                xmlElement = objectFactory.createBehandlingAvsluttet((BehandlingAvsluttet) behandlingStatus);
            } else {
                throw new IntegrasjonException("Ukjent Behandlingsstatus i kø til Sak og Behandling");
            }
            Marshaller marshaller = jaxb2Marshaller.createMarshaller();
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
