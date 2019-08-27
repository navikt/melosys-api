package no.nav.melosys.domain.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.util.LovvalgBestemmelseUtils;

public final class LovvalgBestemmelseDeserializer extends StdDeserializer<LovvalgBestemmelse> {

    public LovvalgBestemmelseDeserializer() {
        super((Class)null);
    }

    @Override
    public LovvalgBestemmelse deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String name = node.textValue();
        return LovvalgBestemmelseUtils.dbDataTilLovvalgBestemmelse(name);
    }
}