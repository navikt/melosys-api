package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import java.time.LocalDateTime;
import javax.jms.Queue;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SakOgBehandlingClientConfigTest.class)
public class SakOgBehandlingClientImplTest {

    private SakOgBehandlingClientImpl sakOgBehandlingClient;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Queue hendelseshåndterer;

    @Before
    public void setUp() {
        sakOgBehandlingClient = new SakOgBehandlingClientImpl(jmsTemplate, hendelseshåndterer);
    }

    @Test
    public void behandlingOpprettetTilXml() throws Exception {
        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medAnsvarligEnhet("MELOSYS");
        builder.medHendelsestidspunkt(LocalDateTime.now());

        sakOgBehandlingClient.sendBehandlingOpprettet(builder.build());

        TextMessage message = (TextMessage) jmsTemplate.receive(hendelseshåndterer);
        assertNotNull(message);
        assertNotNull(message.getText());
    }

    @Test
    public void sendBehandlingAvsluttetTilKø() throws Exception {
        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medAnsvarligEnhet("MELOSYS");
        builder.medHendelsestidspunkt(LocalDateTime.now());

        sakOgBehandlingClient.sendBehandlingAvsluttet(builder.build());

        TextMessage message = (TextMessage) jmsTemplate.receive(hendelseshåndterer);
        assertNotNull(message);
        assertNotNull(message.getText());
    }

}
