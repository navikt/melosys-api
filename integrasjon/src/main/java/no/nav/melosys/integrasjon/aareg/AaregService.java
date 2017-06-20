package no.nav.melosys.integrasjon.aareg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdConsumer;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold;

public class AaregService implements AaregFasade {

    private static final Logger log = LoggerFactory.getLogger(AaregService.class);

    private ArbeidsforholdConsumer arbeidsforholdConsumer;

    @Autowired
    public AaregService(ArbeidsforholdConsumer arbeidsforholdConsumer) {
        this.arbeidsforholdConsumer = arbeidsforholdConsumer;
    }

    public Arbeidsforhold finnArbeidsforholdPrArbeidstaker() {
        return new Arbeidsforhold();
    }

}
