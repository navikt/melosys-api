package no.nav.melosys.integrasjon.dokgen;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.integrasjon.dokgen.dto.MangelbrevBruker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DokgenConsumerTest {

    private WireMockServer wireMockServer;
    private DokgenConsumer dokgenConsumer;

    @BeforeAll
    public void setup() throws Exception {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build();

        dokgenConsumer = new DokgenConsumer(webClient);
    }

    @AfterAll
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void skalBestilleBrev() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withQueryParam("somKopi", equalTo("false"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), false, false)).isNotNull();
    }

    @Test
    void skalBestilleBrevSomKopi() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withQueryParam("somKopi", equalTo("true"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, false)).isNotNull();
    }

    @Test
    void skalBestilleBrevSomUtkast() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withQueryParam("somKopi", equalTo("true"))
            .withQueryParam("utkast", equalTo("true"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, true)).isNotNull();
    }

    private MangelbrevBruker getMangelbrevBruker() {
        MangelbrevBrevbestilling mangelbrevBrevbestilling = new MangelbrevBrevbestilling.Builder()
            .medBehandling(lagBehandling())
            .build();
        return MangelbrevBruker.av(mangelbrevBrevbestilling);
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(lagFagsak(behandling));
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setSaksopplysninger(Collections.singleton(lagPersondokument()));
        return behandling;
    }

    private Fagsak lagFagsak(Behandling behandling) {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        behandling.setType(Behandlingstyper.SOEKNAD);
        fagsak.setBehandlinger(List.of(behandling));
        return fagsak;
    }

    private Saksopplysning lagPersondokument() {
        Saksopplysning person = new Saksopplysning();
        PersonDokument personDok = new PersonDokument();
        person.setDokument(personDok);
        person.setType(SaksopplysningType.PERSOPL);

        return person;
    }
}
