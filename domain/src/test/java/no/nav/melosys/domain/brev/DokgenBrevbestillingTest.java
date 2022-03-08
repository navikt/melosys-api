package no.nav.melosys.domain.brev;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
    void deserialsiering_skalBliRiktigType_AvslagBrevbestilling() throws JsonProcessingException {
        String dataString = """
                {
                    "avslagFritekst": "avslagFritekst"
            }
            """;

        DokgenBrevbestilling dokgenBrevbestilling = dataMapper.readValue(dataString, DokgenBrevbestilling.class);
        assertThat(dokgenBrevbestilling).isInstanceOf(AvslagBrevbestilling.class);
    }

    @Test
    void deserialsiering_skalBliRiktigType_FritekstbrevBrevbestilling() throws JsonProcessingException {
        String dataString = """
                {
                    "fritekstTittel": "",
                    "fritekst": "",
                    "kontaktopplysninger": false,
                    "navnFullmektig": ""
            }
            """;

        DokgenBrevbestilling dokgenBrevbestilling = dataMapper.readValue(dataString, DokgenBrevbestilling.class);
        assertThat(dokgenBrevbestilling).isInstanceOf(FritekstbrevBrevbestilling.class);
    }

    @Test
    void deserialsiering_skalBliRiktigType_InnvilgelseBrevbestilling() throws JsonProcessingException {
        String dataString = """
                {
                    "innledningFritekst": "",
                    "begrunnelseFritekst": "",
                    "ektefelleFritekst": "",
                    "barnFritekst": "",
                    "virksomhetArbeidsgiverSkalHaKopi": false,
                    "nyVurderingBakgrunn": "false"
            }
            """;

        DokgenBrevbestilling dokgenBrevbestilling = dataMapper.readValue(dataString, DokgenBrevbestilling.class);
        assertThat(dokgenBrevbestilling).isInstanceOf(InnvilgelseBrevbestilling.class);
    }

    @Test
    void deserialsiering_skalBliRiktigType_MangelbrevBrevbestilling() throws JsonProcessingException {
        String dataString = """
                {
                    "manglerInfoFritekst": "",
                    "innledningFritekst": "",
                    "fullmektigNavn": ""
            }
            """;

        DokgenBrevbestilling dokgenBrevbestilling = dataMapper.readValue(dataString, DokgenBrevbestilling.class);
        assertThat(dokgenBrevbestilling).isInstanceOf(MangelbrevBrevbestilling.class);
    }
}
