package no.nav.melosys.integrasjon.sakogbehandling;

import java.time.LocalDateTime;
import javax.jms.JMSException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

// FIXME: Ad-hoc integrasjonstesting av kø
@Ignore
public class SakOgBehandlingClientTestIT {

    private SakOgBehandlingClient client;

    @Before
    public void setUp() {
        SakOgBehandlingClientConfig config = new SakOgBehandlingClientConfig();

        ReflectionTestUtils.setField(config, "hostName", "d26apvl258.test.local");
        ReflectionTestUtils.setField(config, "port", 1412);
        ReflectionTestUtils.setField(config, "queueManager", "MTXLSC02");
        ReflectionTestUtils.setField(config, "channel", "T5_MELOSYS");
        ReflectionTestUtils.setField(config, "queueName", "QA.T5_SBEH.SAKSBEHANDLING");

        try {
            client = new SakOgBehandlingClientImpl(config.jmsTemplate(), config.hendelseshåndterer());
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void notPing() throws Exception {
        XMLGregorianCalendar hendelsesTidspunkt = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(LocalDateTime.now());
        BehandlingOpprettet behandlingOpprettet = new BehandlingOpprettet();
        behandlingOpprettet.setAnsvarligEnhetREF("MELOSYS");
        behandlingOpprettet.setHendelsesTidspunkt(hendelsesTidspunkt);

        client.sendBehandlingOpprettet(behandlingOpprettet);
    }
}
