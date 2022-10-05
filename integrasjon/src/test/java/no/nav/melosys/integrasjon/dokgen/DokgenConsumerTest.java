package no.nav.melosys.integrasjon.dokgen;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.person.Persondata;
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
    void lagPdf_skalBestilleBrev() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withQueryParam("somKopi", equalTo("false"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), false, false)).isNotNull();
    }

    @Test
    void lagPdf_skalBestilleBrevSomKopi() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withQueryParam("somKopi", equalTo("true"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, false)).isNotNull();
    }

    @Test
    void lagPdf_skalBestilleBrevSomUtkast() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withQueryParam("somKopi", equalTo("true"))
            .withQueryParam("utkast", equalTo("true"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdf("mangelbrev_bruker", getMangelbrevBruker(), true, true)).isNotNull();
    }

    @Test
    void lagPdfMedVedlegg_skalBestilleBrevMedVedlegg() {
        byte[] vedleggHeihei = "heihei".getBytes(StandardCharsets.UTF_8);
        byte[] vedleggTeit = "teit".getBytes(StandardCharsets.UTF_8);

        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withHeader("content-type", containing("multipart/form-data"))
            .withQueryParam("somKopi", equalTo("false"))
            .withMultipartRequestBody(aMultipart()
                .withName("vedlegg")
                .withBody(containing(Base64.getEncoder().encodeToString(vedleggHeihei)))
                .withBody(containing(Base64.getEncoder().encodeToString(vedleggTeit))))
            .withMultipartRequestBody(aMultipart()
                .withName("metadata")
                .withBody(matchingJsonPath("saksopplysninger")))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdfMedVedlegg("mangelbrev_bruker",
            getMangelbrevBruker(),
            false,
            false,
            Arrays.asList(vedleggHeihei, vedleggTeit))).isNotNull();
    }

    @Test
    void lagPdfMedVedlegg_skalBestilleBrevSomKopi() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withQueryParam("somKopi", equalTo("true"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdfMedVedlegg("mangelbrev_bruker",
            getMangelbrevBruker(),
            true,
            false,
            Collections.emptyList())).isNotNull();
    }

    @Test
    void lagPdfMedVedlegg_skalBestilleBrevSomUtkast() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/mangelbrev_bruker/lag-pdf"))
            .withQueryParam("somKopi", equalTo("true"))
            .withQueryParam("utkast", equalTo("true"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdfMedVedlegg("mangelbrev_bruker",
            getMangelbrevBruker(),
            true,
            true,
            Collections.emptyList())).isNotNull();
    }

    private MangelbrevBruker getMangelbrevBruker() {
        MangelbrevBrevbestilling mangelbrevBrevbestilling = new MangelbrevBrevbestilling.Builder()
            .medBehandling(lagBehandling())
            .medPersonDokument((Persondata) lagPersondokument().getDokument())
            .medPersonMottaker((Persondata) lagPersondokument().getDokument())
            .build();
        return MangelbrevBruker.av(mangelbrevBrevbestilling, Instant.now());
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
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
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
