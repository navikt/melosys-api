package no.nav.melosys.domain.brev;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.ftrl.Betalingsstatus;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype;
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Sjekker at alle subtyper av DokgenBrevbestilling kan deserialiseres til riktig type.
 * Eksempel: Dersom alle feltene til subklasse X også inneholder i subklasse Z, vil deserialiseringen feile.
 */
class DokgenBrevbestillingTest {
    private final ObjectMapper dataMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new SimpleModule().addDeserializer(LovvalgBestemmelse.class, new LovvalgBestemmelseDeserializer()));

    @Test
    void deserialsiering_skalBliRiktigType_forAlleSubtyperAvDokgenBrevbestilling() throws JsonProcessingException {
        JsonSubTypes jsonSubTypes = DokgenBrevbestilling.class.getAnnotation(JsonSubTypes.class);
        var classes = Arrays.stream(jsonSubTypes.value()).map(JsonSubTypes.Type::value).toList();

        for (var type : classes) {
            ObjectNode node = getJsonNodes(type);
            DokgenBrevbestilling dokgenBrevbestilling = dataMapper.readValue(node.toPrettyString(), DokgenBrevbestilling.class);
            assertThat(dokgenBrevbestilling).isInstanceOf(type);
        }
    }

    private ObjectNode getJsonNodes(Class type) {
        var node = dataMapper.createObjectNode();
        for (var a : type.getDeclaredFields()) {
            node.put(a.getName(), a.getType().getSimpleName());
            if (a.getType().getSimpleName().equals("boolean")) {
                node.put(a.getName(), false);
            } else if (a.getType().getSimpleName().equals("LocalDate")) {
                node.put(a.getName(), "2022-02-02");
            } else if (a.getType().getSimpleName().equals(Mottakerroller.class.getSimpleName())) {
                node.put(a.getName(), Mottakerroller.NORSK_MYNDIGHET.name());
            } else if (a.getType().getSimpleName().equals(Ikkeyrkesaktivsituasjontype.class.getSimpleName())) {
                node.put(a.getName(), Ikkeyrkesaktivsituasjontype.ANNET.name());
            } else if (a.getType().getSimpleName().equals(Betalingsstatus.class.getSimpleName())) {
                node.put(a.getName(), Betalingsstatus.DELVIS_BETALT.name());
            } else if (a.getType().getSimpleName().equals(Representerer.class.getSimpleName())) {
                node.put(a.getName(), Representerer.BRUKER.name());
            } else {
                node.put(a.getName(), a.getType().getSimpleName());
            }
        }
        return node;
    }
}
