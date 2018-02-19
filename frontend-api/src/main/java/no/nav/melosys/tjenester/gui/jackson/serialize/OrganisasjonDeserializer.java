package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;

public class OrganisasjonDeserializer extends StdDeserializer<OrganisasjonDokument> {

    public OrganisasjonDeserializer() {
        super(OrganisasjonDokument.class);
    }

    @Override
    public OrganisasjonDokument deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        OrganisasjonDokument dokument = new OrganisasjonDokument();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        dokument.setNavn(Collections.singletonList(node.get("navn").asText()));
        dokument.setOrgnummer(node.get("orgnr").asText());

        if (node.has("forretningsadresse")) {
            // FIXME: Deserialiser forretningsadresse
        }

        if (node.has("postadresse")) {
            // FIXME: Deserialiser postadresse
        }

        return dokument;
    }
}
