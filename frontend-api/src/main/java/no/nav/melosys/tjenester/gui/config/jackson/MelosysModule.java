package no.nav.melosys.tjenester.gui.config.jackson;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.module.SimpleModule;
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
            public ValueDeserializer<?> findEnumDeserializer(JavaType type, DeserializationConfig config, BeanDescription.Supplier beanDescSupplier) {
                if (Kodeverk.class.isAssignableFrom(type.getRawClass())) {
                    return new KodeDeserializer((Class<? extends Kodeverk>) type.getRawClass());
                }
                return null;
            }

            @Override
            public ValueDeserializer<?> findBeanDeserializer(JavaType type, DeserializationConfig config, BeanDescription.Supplier beanDescSupplier) {
                if (LovvalgBestemmelse.class.isAssignableFrom(type.getRawClass())) {
                    return new LovvalgBestemmelseDeserializer();
                }
                return null;
            }
        });
    }
}
