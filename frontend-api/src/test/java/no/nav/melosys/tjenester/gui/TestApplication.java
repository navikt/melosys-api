package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestApplication {

    @Bean
    public KodeverkRegister kodeverkRegisterStub() {
        return kodeverkNavn -> {
            Kode kode = new Kode("DUMMY", "DUMMY", LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
            Map<String, List<Kode>> kodeMap = Map.of("DUMMY", List.of(kode));
            return new Kodeverk("DUMMY", kodeMap);
        };
    }

    @Bean
    public KodeverkService kodeverkService(KodeverkRegister kodeverkRegister) {
        return new KodeverkService(kodeverkRegister);
    }


}
