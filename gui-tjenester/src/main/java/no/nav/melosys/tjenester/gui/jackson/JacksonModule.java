package no.nav.melosys.tjenester.gui.jackson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.serialize.LandkodeSerializer;
import no.nav.melosys.tjenester.gui.jackson.serialize.OrganisasjonSerializer;

@Component
public class JacksonModule extends SimpleModule {

    @Autowired
    public JacksonModule(KodeverkService kodeverkService) {
        super();

        addSerializer(new LandkodeSerializer(kodeverkService));
        addSerializer(new OrganisasjonSerializer(kodeverkService));

    }
}
