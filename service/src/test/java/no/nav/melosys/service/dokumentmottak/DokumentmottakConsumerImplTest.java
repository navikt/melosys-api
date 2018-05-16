package no.nav.melosys.service.dokumentmottak;

import com.mockrunner.mock.jms.MockTextMessage;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DokumentmottakConsumerImplTest {

    private Meldingsfordeler meldingsfordeler;

    private DokumentmottakConsumerImpl consumer;

    @Before
    public void setUp() {
        meldingsfordeler = mock(ProsessinstansMeldingsfordeler.class);
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

        doAnswer(invocation -> {
            Object argument = invocation.getArgument(0);
            assertThat(argument).isNotNull();
            assertThat(argument).isInstanceOf(ForsendelsesinformasjonDto.class);
            return null;
        }).when(meldingsfordeler).execute(any());

        consumer.mottaDokument(new MockTextMessage(xml));
    }

}
