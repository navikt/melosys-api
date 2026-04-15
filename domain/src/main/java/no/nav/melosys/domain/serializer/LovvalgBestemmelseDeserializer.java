package no.nav.melosys.domain.serializer;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;

public final class LovvalgBestemmelseDeserializer extends StdDeserializer<LovvalgBestemmelse> {

    public LovvalgBestemmelseDeserializer() {
        super(LovvalgBestemmelse.class);
    }

    @Override
    public LovvalgBestemmelse deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JsonNode node = jsonParser.readValueAsTree();
        String name = node.textValue();
        return LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse(name);
    }
}