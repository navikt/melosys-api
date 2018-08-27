package no.nav.melosys.tjenester.gui.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.serialize.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JacksonModule extends SimpleModule {

    @Autowired
    public JacksonModule(KodeverkService kodeverkService) {
        super();

        addSerializer(new FellesKodeverkSerializer(kodeverkService));
        addSerializer(new KodeSerializer());
        addSerializer(new MedlemsperiodeSerializer(kodeverkService));
        addSerializer(new OrganisasjonSerializer(kodeverkService));
        addSerializer(new PersonSerializer());
        addSerializer(new PersonhistorikkSerializer());
    }
}
