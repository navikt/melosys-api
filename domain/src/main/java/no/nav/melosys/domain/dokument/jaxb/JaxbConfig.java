package no.nav.melosys.domain.dokument.jaxb;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import no.nav.melosys.domain.dokument.Dokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.person.PersonopplysningDokument;

@Configuration
public class JaxbConfig {

    /**
     * Spring JAXB 2 marshaller/unmarshaller. OBS Ikke thread-safe
     * @return Jaxb2Marshaller
     */
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        Class<? extends Dokument>[] classes = new Class[]{ArbeidsforholdDokument.class, PersonopplysningDokument.class};
        marshaller.setClassesToBeBound(classes);
        return marshaller;
    }

}
