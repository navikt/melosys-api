package no.nav.melosys.tjenester.gui.config.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.config.jackson.deserialize.KodeDeserializer;
import no.nav.melosys.tjenester.gui.config.jackson.serialize.*;

public class MelosysModule extends SimpleModule {

    public MelosysModule(KodeverkService kodeverkService) {
        super();

        addSerializer(new FellesKodeverkSerializer(kodeverkService));
        addSerializer(new KodeSerializer());
        addSerializer(new MedlemsperiodeSerializer(kodeverkService));
        addSerializer(new MidlertidigPostadresseSerializer(kodeverkService));
        addSerializer(new OrganisasjonSerializer(kodeverkService));

        addDeserializer(LovvalgBestemmelse.class, new LovvalgBestemmelseDeserializer());

        setDeserializers(new SimpleDeserializers() {
            @Override
            @SuppressWarnings("unchecked")
            public JsonDeserializer<?> findEnumDeserializer(Class<?> type, DeserializationConfig config, BeanDescription beanDesc) {
                if (Kodeverk.class.isAssignableFrom(type)) {
                    return new KodeDeserializer((Class<? extends Kodeverk>) type);
                }
                return null;
            }
        });
    }
}
