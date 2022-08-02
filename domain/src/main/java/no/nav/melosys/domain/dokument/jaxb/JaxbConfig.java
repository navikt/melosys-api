package no.nav.melosys.domain.dokument.jaxb;

import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

@Configuration
public class JaxbConfig {
    private static final Logger log = LoggerFactory.getLogger(JaxbConfig.class);
    private final Jaxb2Marshaller jaxb2Marshaller;

    public JaxbConfig() {
        this.jaxb2Marshaller = createMarshaller();
    }

    @Bean
    @Scope(SCOPE_SINGLETON)
    public Jaxb2Marshaller jaxb2Marshaller() {
        return jaxb2Marshaller;
    }

    private static Jaxb2Marshaller createMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("no.nav.melosys.domain.dokument", "no.nav.dok.melosysbrev", "no.nav.tjeneste.virksomhet");
        marshaller.setValidationEventHandler(new DefaultValidationEventHandler());
        return marshaller;
    }

    public static Jaxb2Marshaller getJaxb2Marshaller() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final Jaxb2Marshaller INSTANCE = createSingleton();

        private static Jaxb2Marshaller createSingleton() {
            Jaxb2Marshaller marshaller = JaxbConfig.createMarshaller();
            try {
                marshaller.afterPropertiesSet();
            } catch (Exception e) {
                log.error("Initialsering av Jaxb2Marshaller feilet: ", e);
            }
            return marshaller;
        }
    }
}
