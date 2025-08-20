package no.nav.melosys.domain.jpa;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
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
    void lastMottatteOpplysninger_erSøknadFtrl_forventTypeSøknadNorgeEllerUtenforEØS() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        mottatteOpplysninger.setJsonData(json);
        mottatteOpplysninger.setType(Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS);
        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger);

        assertThat(mottatteOpplysninger.getMottatteOpplysningerData()).isNotNull();
        assertThat(mottatteOpplysninger.getMottatteOpplysningerData()).isInstanceOf(SøknadNorgeEllerUtenforEØS.class);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, mottatteOpplysninger.getMottatteOpplysningerData());
    }

    @Test
    void lastMottatteOpplysninger_erSøknadTrygdeavtale_forventTypeSøknadNorgeEllerUtenforEØS() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        mottatteOpplysninger.setJsonData(json);
        mottatteOpplysninger.setType(Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS);
        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger);

        assertThat(mottatteOpplysninger.getMottatteOpplysningerData()).isNotNull();
        assertThat(mottatteOpplysninger.getMottatteOpplysningerData()).isInstanceOf(SøknadNorgeEllerUtenforEØS.class);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, mottatteOpplysninger.getMottatteOpplysningerData());
    }

    @Test
    void lastMottatteOpplysninger_erSøknad_forventTypeSoeknad() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        mottatteOpplysninger.setJsonData(json);
        mottatteOpplysninger.setType(Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger);

        MottatteOpplysningerData data = mottatteOpplysninger.getMottatteOpplysningerData();
        assertThat(data).isNotNull().isInstanceOf(Soeknad.class);

        Soeknad søknad = (Soeknad) data;
        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, søknad);
        assertThat(søknad.getArbeidsgiversBekreftelse().getArbeidsgiverBekrefterUtsendelse())
            .isEqualTo(jsonNode.get("arbeidsgiversBekreftelse").get("arbeidsgiverBekrefterUtsendelse").booleanValue());
        assertThat(søknad.getLoennOgGodtgjoerelse().getBruttoLoennPerMnd()).isEqualTo(
            new BigDecimal(jsonNode.get("loennOgGodtgjoerelse").get("bruttoLoennPerMnd").asText()));
    }

    private void assertKonvertering(JsonNode jsonNode, MottatteOpplysningerData data) {
        assertThat(data.arbeidPaaLand.getFysiskeArbeidssteder())
            .hasSize(jsonNode.get("arbeidPaaLand").withArray("fysiskeArbeidssteder").size());
        assertThat(data.foretakUtland).hasSize(jsonNode.withArray("foretakUtland").size());
        assertThat(data.maritimtArbeid).hasSize(jsonNode.withArray("maritimtArbeid").size());
        assertThat(data.bosted.getIntensjonOmRetur()).isEqualTo(jsonNode.get("bosted").get("intensjonOmRetur").booleanValue());
        assertThat(data.personOpplysninger.getUtenlandskIdent())
            .hasSize(jsonNode.get("personOpplysninger").withArray("utenlandskIdent").size());
        assertThat(data.selvstendigArbeid.getSelvstendigForetak().get(0).getOrgnr())
            .isEqualTo(jsonNode.get("selvstendigArbeid").withArray("selvstendigForetak").get(0).get("orgnr").textValue());
    }
}
