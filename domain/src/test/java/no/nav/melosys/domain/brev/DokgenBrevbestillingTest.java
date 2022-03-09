package no.nav.melosys.domain.brev;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DokgenBrevbestillingTest {
    private final ObjectMapper dataMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new SimpleModule().addDeserializer(LovvalgBestemmelse.class, new LovvalgBestemmelseDeserializer()));

    @Test
    void deserialsiering_skalBliRiktigType_forAlleSubtyperAvDokgenBrevbestilling() throws JsonProcessingException {
        var subTypesOfDokgenBrevbestilling = List.of(
            MangelbrevBrevbestilling.class,
            InnvilgelseBrevbestilling.class,
            FritekstbrevBrevbestilling.class,
            AvslagBrevbestilling.class);
        for (var type : subTypesOfDokgenBrevbestilling) {
            ObjectNode node = getJsonNodes(type);
            DokgenBrevbestilling dokgenBrevbestilling = dataMapper.readValue(node.toPrettyString(), DokgenBrevbestilling.class);
            assertThat(dokgenBrevbestilling).isInstanceOf(type);
        }
    }

    private ObjectNode getJsonNodes(Class<? extends DokgenBrevbestilling> type) {
        var node = dataMapper.createObjectNode();
        for (var a : type.getDeclaredFields()) {
            node.put(a.getName(), a.getType().getSimpleName());
            if (a.getType().getSimpleName().equals("boolean")) {
                node.put(a.getName(), false);
            } else {
                node.put(a.getName(), a.getType().getSimpleName());
            }
        }
        return node;
    }
}
