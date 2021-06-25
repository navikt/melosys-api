package no.nav.melosys.integrasjon.aareg.arbeidsforhold;


import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArbeidsforholdKodeOppslagConfig implements RestConsumer {
    private static final Logger log = LoggerFactory.getLogger(ArbeidsforholdKodeOppslagConfig.class);
    private final KodeverkRegister kodeverkRegister;

    @Autowired
    public ArbeidsforholdKodeOppslagConfig(KodeverkRegister kodeverkRegister) {
        this.kodeverkRegister = kodeverkRegister;
    }

    @Bean
    KodeOppslag getKodeOKodeverkServiceppslag() {
        return new KodeOppslagFraKodeVerk(kodeverkRegister);
    }
}
