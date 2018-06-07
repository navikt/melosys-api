package no.nav.melosys.integrasjon.sakogbehandling;

import java.time.LocalDateTime;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.felles.QueueConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// FIXME: Ad-hoc integrasjonstesting av kø (ekskluder SakOgBehandlingClientConfigTest fra component scan)
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
public class SakOgBehandlingClientTestIT {

    @Autowired
    private SakOgBehandlingClient client;

    @Test
    public void notPing() throws Exception {
        XMLGregorianCalendar hendelsesTidspunkt = KonverteringsUtils.localDateTimeToXMLGregorianCalendar(LocalDateTime.now());
        BehandlingOpprettet behandlingOpprettet = new BehandlingOpprettet();
        behandlingOpprettet.setAnsvarligEnhetREF("MELOSYS");
        behandlingOpprettet.setHendelsesTidspunkt(hendelsesTidspunkt);

        client.sendBehandlingOpprettet(behandlingOpprettet);
    }

    @Configuration
    @ComponentScan(basePackageClasses = {QueueConfig.class, SakOgBehandlingClientImpl.class})
    @PropertySource("classpath:test.properties")
    public static class SakOgBehandlingClientConfigTest {}
}
