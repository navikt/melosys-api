package no.nav.melosys.domain.jpa;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.mottatteopplysninger.*;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class MottatteOpplysningerListenerTest {

    private final MottatteOpplysningerListener mottatteOpplysningerListener = new MottatteOpplysningerListener();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MottatteOpplysninger mottatteOpplysninger;

    @BeforeEach
    public void setup() {
        mottatteOpplysninger = new MottatteOpplysninger();
    }

    @Test
    void lastMottatteOpplysninger_erSøknadFtrl_forventTypeSoeknadFtrl() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        mottatteOpplysninger.setJsonData(json);
        mottatteOpplysninger.setType(Behandlingsgrunnlagtyper.SØKNAD_FOLKETRYGDEN);
        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger);

        assertThat(mottatteOpplysninger.getMottatteOpplysningerData()).isNotNull();
        assertThat(mottatteOpplysninger.getMottatteOpplysningerData()).isInstanceOf(SoeknadFtrl.class);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, mottatteOpplysninger.getMottatteOpplysningerData());
    }

    @Test
    void lastMottatteOpplysninger_erSøknadTrygdeavtale_forventTypeSoeknadTrygdeavtale() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        mottatteOpplysninger.setJsonData(json);
        mottatteOpplysninger.setType(Behandlingsgrunnlagtyper.SØKNAD_TRYGDEAVTALE);
        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger);

        assertThat(mottatteOpplysninger.getMottatteOpplysningerData()).isNotNull();
        assertThat(mottatteOpplysninger.getMottatteOpplysningerData()).isInstanceOf(SoeknadTrygdeavtale.class);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, mottatteOpplysninger.getMottatteOpplysningerData());
    }

    @Test
    void lastMottatteOpplysninger_erSøknad_forventTypeSoeknad() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        mottatteOpplysninger.setJsonData(json);
        mottatteOpplysninger.setType(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger);

        MottatteOpplysningerData data = mottatteOpplysninger.getMottatteOpplysningerData();
        assertThat(data).isNotNull().isInstanceOf(Soeknad.class);

        Soeknad søknad = (Soeknad) data;
        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, søknad);
        assertThat(søknad.arbeidsgiversBekreftelse.arbeidsgiverBekrefterUtsendelse)
            .isEqualTo(jsonNode.get("arbeidsgiversBekreftelse").get("arbeidsgiverBekrefterUtsendelse").booleanValue());
        assertThat(søknad.loennOgGodtgjoerelse.bruttoLoennPerMnd).isEqualTo(
            new BigDecimal(jsonNode.get("loennOgGodtgjoerelse").get("bruttoLoennPerMnd").asText()));
    }

    private void assertKonvertering(JsonNode jsonNode, MottatteOpplysningerData data) {
        assertThat(data.arbeidPaaLand.fysiskeArbeidssteder.size())
            .isEqualTo(jsonNode.get("arbeidPaaLand").withArray("fysiskeArbeidssteder").size());
        assertThat(data.foretakUtland.size()).isEqualTo(jsonNode.withArray("foretakUtland").size());
        assertThat(data.maritimtArbeid.size()).isEqualTo(jsonNode.withArray("maritimtArbeid").size());
        assertThat(data.bosted.intensjonOmRetur).isEqualTo(jsonNode.get("bosted").get("intensjonOmRetur").booleanValue());
        assertThat(data.personOpplysninger.utenlandskIdent.size())
            .isEqualTo(jsonNode.get("personOpplysninger").withArray("utenlandskIdent").size());
        assertThat(data.selvstendigArbeid.selvstendigForetak.get(0).orgnr)
            .isEqualTo(jsonNode.get("selvstendigArbeid").withArray("selvstendigForetak").get(0).get("orgnr").textValue());
    }
}
