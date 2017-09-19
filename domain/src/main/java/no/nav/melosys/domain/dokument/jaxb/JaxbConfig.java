package no.nav.melosys.domain.dokument.jaxb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.person.PersonopplysningDokument;

@Configuration
public class JaxbConfig {

    @Bean
    Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        Class[] classes = {ArbeidsforholdDokument.class, PersonopplysningDokument.class};
        marshaller.setClassesToBeBound(classes);
        return marshaller;
    }


}
