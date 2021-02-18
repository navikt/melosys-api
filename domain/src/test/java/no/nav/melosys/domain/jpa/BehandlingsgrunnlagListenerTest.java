package no.nav.melosys.domain.jpa;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class BehandlingsgrunnlagListenerTest {

    private final BehandlingsgrunnlagListener behandlingsgrunnlagListener = new BehandlingsgrunnlagListener();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Behandlingsgrunnlag behandlingsgrunnlag;

    @BeforeEach
    public void setup() {
        behandlingsgrunnlag = new Behandlingsgrunnlag();
    }

    @Test
    void lastBehandlingsgrunnlag_erSøknadFtrl_forventTypeSoeknadFtrl() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        behandlingsgrunnlag.setJsonData(json);
        behandlingsgrunnlag.setType(Behandlingsgrunnlagtyper.SØKNAD_FOLKETRYGDEN);
        behandlingsgrunnlagListener.lastBehandlingsgrunnlag(behandlingsgrunnlag);

        assertThat(behandlingsgrunnlag.getBehandlingsgrunnlagdata()).isNotNull();
        assertThat(behandlingsgrunnlag.getBehandlingsgrunnlagdata()).isInstanceOf(SoeknadFtrl.class);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, behandlingsgrunnlag.getBehandlingsgrunnlagdata());
    }

    @Test
    void lastBehandlingsgrunnlag_erSøknad_forventTypeSoeknad() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        behandlingsgrunnlag.setJsonData(json);
        behandlingsgrunnlag.setType(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        behandlingsgrunnlagListener.lastBehandlingsgrunnlag(behandlingsgrunnlag);

        BehandlingsgrunnlagData data = behandlingsgrunnlag.getBehandlingsgrunnlagdata();
        assertThat(data).isNotNull().isInstanceOf(Soeknad.class);

        Soeknad søknad = (Soeknad) data;
        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, søknad);
        assertThat(søknad.arbeidsgiversBekreftelse.arbeidsgiverBekrefterUtsendelse)
            .isEqualTo(jsonNode.get("arbeidsgiversBekreftelse").get("arbeidsgiverBekrefterUtsendelse").booleanValue());
    }

    @Test
    void lastBehandlingsgrunnlag_erGenerelt_forventBehGrunnlag() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        behandlingsgrunnlag.setJsonData(json);
        behandlingsgrunnlag.setType(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        behandlingsgrunnlagListener.lastBehandlingsgrunnlag(behandlingsgrunnlag);

        assertThat(behandlingsgrunnlag.getBehandlingsgrunnlagdata()).isNotNull();
        assertThat(behandlingsgrunnlag.getBehandlingsgrunnlagdata()).isInstanceOf(BehandlingsgrunnlagData.class);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, behandlingsgrunnlag.getBehandlingsgrunnlagdata());
    }

    private void assertKonvertering(JsonNode jsonNode, BehandlingsgrunnlagData data) {
        assertThat(data.arbeidPaaLand.fysiskeArbeidssteder.size())
            .isEqualTo(jsonNode.get("arbeidPaaLand").withArray("fysiskeArbeidssteder").size());
        assertThat(data.foretakUtland.size()).isEqualTo(jsonNode.withArray("foretakUtland").size());
        assertThat(data.maritimtArbeid.size()).isEqualTo(jsonNode.withArray("maritimtArbeid").size());
        assertThat(data.bosted.intensjonOmRetur).isEqualTo(jsonNode.get("bosted").get("intensjonOmRetur").booleanValue());
        assertThat(data.personOpplysninger.utenlandskIdent.size()).isEqualTo(jsonNode.get("personOpplysninger").withArray("utenlandskIdent").size());
        assertThat(data.selvstendigArbeid.selvstendigForetak.get(0).orgnr).isEqualTo(jsonNode.get("selvstendigArbeid").withArray("selvstendigForetak").get(0).get("orgnr").textValue());
    }
}