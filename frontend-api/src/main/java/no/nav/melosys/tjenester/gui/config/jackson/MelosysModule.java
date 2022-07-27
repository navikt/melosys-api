package no.nav.melosys.tjenester.gui.config.jackson;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.module.SimpleModule;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.config.jackson.deserialize.LocalDateDeserializer;
import no.nav.melosys.tjenester.gui.config.jackson.serialize.*;

public class MelosysModule extends SimpleModule {

    public MelosysModule(KodeverkService kodeverkService) {
        super();

        addSerializer(new FellesKodeverkSerializer(kodeverkService));
        addSerializer(new KodeSerializer());
        addSerializer(new MedlemsperiodeSerializer(kodeverkService));
        addSerializer(new MidlertidigPostadresseSerializer(kodeverkService));
        addSerializer(new OrganisasjonSerializer(kodeverkService));
        addDeserializer(LocalDate.class, new LocalDateDeserializer());
    }
}
