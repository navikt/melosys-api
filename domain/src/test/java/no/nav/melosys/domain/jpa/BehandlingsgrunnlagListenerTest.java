package no.nav.melosys.domain.jpa;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BehandlingsgrunnlagListenerTest {

    private final BehandlingsgrunnlagListener behandlingsgrunnlagListener = new BehandlingsgrunnlagListener();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Behandlingsgrunnlag behandlingsgrunnlag;

    @Before
    public void setup() {
        behandlingsgrunnlag = new Behandlingsgrunnlag();
    }

    @Test
    public void lastBehandlingsgrunnlag_erSøknad_forventTypeSoeknadDokument() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        behandlingsgrunnlag.setJsonData(json);
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.SØKNAD);
        behandlingsgrunnlagListener.lastBehandlingsgrunnlag(behandlingsgrunnlag);

        BehandlingsgrunnlagData data = behandlingsgrunnlag.getBehandlingsgrunnlagdata();
        assertThat(data).isNotNull();
        assertThat(data).isInstanceOf(SoeknadDokument.class);

        SoeknadDokument søknad = (SoeknadDokument) data;
        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, søknad);
        assertThat(søknad.arbeidsgiversBekreftelse.arbeidsgiverBekrefterUtsendelse)
            .isEqualTo(jsonNode.get("arbeidsgiversBekreftelse").get("arbeidsgiverBekrefterUtsendelse").booleanValue());
    }

    @Test
    public void lastBehandlingsgrunnlag_erGenerelt_forventBehGrunnlag() throws URISyntaxException, IOException {
        URI søknadURI = (getClass().getClassLoader().getResource("soeknad/soeknad.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(søknadURI)));

        behandlingsgrunnlag.setJsonData(json);
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.GENERELT);
        behandlingsgrunnlagListener.lastBehandlingsgrunnlag(behandlingsgrunnlag);

        assertThat(behandlingsgrunnlag.getBehandlingsgrunnlagdata()).isNotNull();
        assertThat(behandlingsgrunnlag.getBehandlingsgrunnlagdata()).isInstanceOf(BehandlingsgrunnlagData.class);

        JsonNode jsonNode = objectMapper.readTree(json);
        assertKonvertering(jsonNode, behandlingsgrunnlag.getBehandlingsgrunnlagdata());
    }

    private void assertKonvertering(JsonNode jsonNode, BehandlingsgrunnlagData data) {
        assertThat(data.arbeidUtland.size()).isEqualTo(jsonNode.withArray("arbeidUtland").size());
        assertThat(data.foretakUtland.size()).isEqualTo(jsonNode.withArray("foretakUtland").size());
        assertThat(data.maritimtArbeid.size()).isEqualTo(jsonNode.withArray("maritimtArbeid").size());
        assertThat(data.bosted.intensjonOmRetur).isEqualTo(jsonNode.get("bosted").get("intensjonOmRetur").booleanValue());
        assertThat(data.personOpplysninger.utenlandskIdent.size()).isEqualTo(jsonNode.get("personOpplysninger").withArray("utenlandskIdent").size());
        assertThat(data.selvstendigArbeid.selvstendigForetak.get(0).orgnr).isEqualTo(jsonNode.get("selvstendigArbeid").withArray("selvstendigForetak").get(0).get("orgnr").textValue());
    }
}