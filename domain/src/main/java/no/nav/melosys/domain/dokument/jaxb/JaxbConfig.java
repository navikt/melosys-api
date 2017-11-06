package no.nav.melosys.domain.dokument.jaxb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.helpers.DefaultValidationEventHandler;

import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;

@Configuration
public class JaxbConfig {

    /**
     * Spring JAXB 2 marshaller/unmarshaller. OBS Ikke thread-safe
     * @return Jaxb2Marshaller
     */
    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(getClassesToBeBound());
        marshaller.setValidationEventHandler(new DefaultValidationEventHandler());
        return marshaller;
    }

    private static Class<?>[] getClassesToBeBound() {
        List<Class<?>> klasser = new ArrayList<>();

        List<Class<? extends SaksopplysningDokument>> dokumentKlasser = Arrays.asList(ArbeidsforholdDokument.class, InntektDokument.class, OrganisasjonDokument.class, PersonDokument.class, MedlemskapDokument.class);
        klasser.addAll(dokumentKlasser);

        return klasser.toArray(new Class[0]);
    }

}
