package no.nav.melosys.domain.dokument.jaxb;

import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class JaxbConfig {

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("no.nav.melosys.domain.dokument", "no.nav.dok.melosysbrev", "no.nav.tjeneste.virksomhet",
            "no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer", "no.nav.melosys.integrasjon.medl.medlemskap");
        marshaller.setValidationEventHandler(new DefaultValidationEventHandler());
        return marshaller;
    }
}
