package no.nav.melosys.integrasjon.aareg.arbeidsforhold;


import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArbeidsforholdKodeOppslagConfig {
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
