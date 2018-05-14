package no.nav.melosys.integrasjon.gsak.dokumentmottak;

import javax.jms.Message;
import javax.jms.TextMessage;

import com.mockrunner.mock.jms.MockQueue;
import no.nav.melding.virksomhet.dokumentnotifikasjon.v1.Forsendelsesinformasjon;
import no.nav.melosys.integrasjon.joark.dokumentmottak.DokumentmottakConsumerConfig;
import no.nav.melosys.integrasjon.joark.dokumentmottak.DokumentmottakConsumerImpl;
import no.nav.melosys.integrasjon.joark.dokumentmottak.Meldingsfordeler;
import no.nav.melosys.integrasjon.joark.dokumentmottak.ProsessinstansMeldingsfordeler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DokumentmottakConfigTest.class)
public class DokumentmottakConsumerImplTest {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private MockQueue mockQueue;

    private DokumentmottakConsumerImpl consumer;

    @Before
    public void setUp() {
        Meldingsfordeler meldingsfordeler = mock(ProsessinstansMeldingsfordeler.class);
        doAnswer((Answer<Void>) invocation -> {
            Object argument = invocation.getArgument(0);
            assertThat(argument).isNotNull();
            assertThat(argument).isInstanceOf(Forsendelsesinformasjon.class);
            return null;
        }).when(meldingsfordeler).execute(any());

        consumer = new DokumentmottakConsumerImpl(meldingsfordeler);
    }

    @Test
    public void sendMessage() throws Exception {
        String xml = "<v1:forsendelsesinformasjon xmlns:v1=\"http://nav.no/melding/virksomhet/dokumentnotifikasjon/v1\">\n" +
            "  <arkivId>MEL-1234</arkivId>\n" +
            "  <arkivsystem>JOARK</arkivsystem>\n" +
            "  <tema kodeverksRef=\"http://nav.no/kodeverk/Kodeverk/Tema\">MED</tema>\n" +
            "  <behandlingstema kodeverksRef=\"http://nav.no/kodeverk/Kodeverk/Behandlingstema\">ab0269</behandlingstema>\n" +
            "</v1:forsendelsesinformasjon>";

        // Ping
        jmsTemplate.send(mockQueue, session -> {
            TextMessage message = session.createTextMessage();
            message.setText(xml);
            return message;
        });

        // Pong
        Message message = jmsTemplate.receive(mockQueue);
        assertThat(message).isNotNull();

        consumer.mottaDokument(message);
    }

}
