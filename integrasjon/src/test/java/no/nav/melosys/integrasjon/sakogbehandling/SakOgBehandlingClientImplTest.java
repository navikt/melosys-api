package no.nav.melosys.integrasjon.sakogbehandling;

import java.time.LocalDateTime;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet;
import no.nav.melosys.integrasjon.KonverteringsUtils;
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
        XMLGregorianCalendar hendelsesTidspunkt = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(LocalDateTime.now());
        BehandlingOpprettet behandlingOpprettet = new BehandlingOpprettet();
        behandlingOpprettet.setAnsvarligEnhetREF("MELOSYS");
        behandlingOpprettet.setHendelsesTidspunkt(hendelsesTidspunkt);

        sakOgBehandlingClient.sendBehandlingOpprettet(behandlingOpprettet);

        TextMessage message = (TextMessage) jmsTemplate.receive(hendelseshåndterer);
        assertNotNull(message);
        assertNotNull(message.getText());
    }

    @Test
    public void sendBehandlingAvsluttetTilKø() throws Exception {
        XMLGregorianCalendar hendelsesTidspunkt = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(LocalDateTime.now());
        BehandlingAvsluttet behandlingAvsluttet = new BehandlingAvsluttet();
        behandlingAvsluttet.setAnsvarligEnhetREF("MELOSYS");
        behandlingAvsluttet.setHendelsesTidspunkt(hendelsesTidspunkt);

        sakOgBehandlingClient.sendBehandlingAvsluttet(behandlingAvsluttet);

        TextMessage message = (TextMessage) jmsTemplate.receive(hendelseshåndterer);
        assertNotNull(message);
        assertNotNull(message.getText());
    }

}
