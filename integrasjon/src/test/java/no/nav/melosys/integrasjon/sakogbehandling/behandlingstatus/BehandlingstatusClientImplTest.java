package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import javax.jms.Queue;
import javax.jms.TextMessage;

import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.exception.IntegrasjonException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = BehandlingstatusClientMockConfig.class)
public class BehandlingstatusClientImplTest {

    private BehandlingstatusClientImpl behandlingstatusClient;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Queue hendelseshåndterer;

    @Before
    public void setUp() throws IntegrasjonException {
        behandlingstatusClient = new BehandlingstatusClientImpl(jmsTemplate, hendelseshåndterer, JaxbConfig.jaxb2Marshaller());
    }

    @Test
    public void behandlingOpprettetTilXml() throws Exception {
        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();

        behandlingstatusClient.sendBehandlingOpprettet(builder.build());

        TextMessage message = (TextMessage) jmsTemplate.receive(hendelseshåndterer);
        assertNotNull(message);
        assertNotNull(message.getText());
    }

    @Test
    public void sendBehandlingAvsluttetTilKø() throws Exception {
        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();

        behandlingstatusClient.sendBehandlingAvsluttet(builder.build());

        TextMessage message = (TextMessage) jmsTemplate.receive(hendelseshåndterer);
        assertNotNull(message);
        assertNotNull(message.getText());
    }

}
