package no.nav.melosys.domain.dokument.jaxb;

import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

@Configuration
public class JaxbConfig {
    private static Jaxb2Marshaller jaxb2Marshaller;

    @Bean
    @Scope(SCOPE_SINGLETON)
    public static Jaxb2Marshaller jaxb2Marshaller() {
        if (jaxb2Marshaller == null) {
            jaxb2Marshaller = initMarshaller();
        }
        return jaxb2Marshaller;
    }

    private static Jaxb2Marshaller initMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("no.nav.melosys.domain.dokument", "no.nav.dok.melosysbrev", "no.nav.tjeneste.virksomhet",
            "no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer", "no.nav.melosys.integrasjon.medl.medlemskap");
        marshaller.setValidationEventHandler(new DefaultValidationEventHandler());
        return marshaller;
    }
}
