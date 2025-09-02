package no.nav.melosys.integrasjon.dokgen;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.integrasjon.dokgen.dto.MangelbrevBruker;
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.InnvilgelseRettigheterPlikterStandardvedlegg;
import no.nav.melosys.integrasjon.dokgen.dto.standardvedlegg.StandardvedleggDto;
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
    void lagPdfForStandardvedlegg_medData_skalBestilleBrev() {
        StandardvedleggDto standardvedlegg = new InnvilgelseRettigheterPlikterStandardvedlegg("Hei");
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/standardvedlegg/lag-pdf"))
            .withQueryParam("somKopi", equalTo("false"))
            .withQueryParam("utkast", equalTo("false"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdfForStandardvedlegg("standardvedlegg", standardvedlegg)).isNotNull();
    }

    @Test
    void lagPdfForStandardvedlegg_utenData_skalBestilleBrev() {
        wireMockServer.stubFor(post(urlPathEqualTo("/mal/standardvedlegg/lag-pdf"))
            .withQueryParam("somKopi", equalTo("false"))
            .withQueryParam("utkast", equalTo("false"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("pdf".getBytes(StandardCharsets.UTF_8)))
        );

        assertThat(dokgenConsumer.lagPdfForStandardvedlegg("standardvedlegg", null)).isNotNull();
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
        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medType(Behandlingstyper.FØRSTEGANG)
            .medSaksopplysninger(Collections.singleton(lagPersondokument()))
            .build();
        behandling.setFagsak(lagFagsak(behandling));
        return behandling;
    }

    private Fagsak lagFagsak(Behandling behandling) {
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        return new Fagsak("MEL-test",
            null,
            Sakstyper.EU_EOS,
            Sakstemaer.MEDLEMSKAP_LOVVALG,
            Saksstatuser.OPPRETTET,
            null,
            Collections.emptySet(),
            List.of(behandling)
        );
    }

    private Saksopplysning lagPersondokument() {
        Saksopplysning person = new Saksopplysning();
        PersonDokument personDok = new PersonDokument();
        person.setDokument(personDok);
        person.setType(SaksopplysningType.PERSOPL);
        return person;
    }
}
