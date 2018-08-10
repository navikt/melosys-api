package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import java.time.LocalDateTime;

import no.nav.melosys.integrasjon.felles.jms.JmsConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

// FIXME: Ad-hoc integrasjonstesting av kø (ekskluder BehandlingstatusClientConfigTest fra component scan)
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
public class BehandlingstatusClientTestIT {

    @Autowired
    private BehandlingstatusClient client;

    @Test
    public void notPing() throws Exception {
        BehandlingStatusMapper.Builder builder = new BehandlingStatusMapper.Builder();
        builder.medAnsvarligEnhet("MELOSYS");
        builder.medHendelsestidspunkt(LocalDateTime.now());

        client.sendBehandlingOpprettet(builder.build());
    }

    @Configuration
    @ComponentScan(basePackageClasses = {JmsConfig.class, BehandlingstatusClientImpl.class})
    @PropertySource("classpath:test.properties")
    public static class BehandlingstatusClientConfigTest {}
}
