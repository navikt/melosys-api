package no.nav.melosys.integrasjon.altinn;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.soknad_altinn.Innhold;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import no.nav.melosys.soknad_altinn.MidlertidigUtsendt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoknadMottakConsumerImplTest {

    private SoknadMottakConsumer soknadMottakConsumer;
    private WireMockServer wireMockServer;

    private final String søknadID = "grj304iht";

    @BeforeAll
    public void initialSetup() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl(wireMockServer.baseUrl())
            .build();

        soknadMottakConsumer = new SoknadMottakConsumerImpl(webClient);
    }

    @BeforeEach
    public void setup() {
        wireMockServer.resetAll();
    }

    @Test
    public void hentSøknad_mottarSoknadIXml_soknadBlirMappetTilStruktur() throws Exception {

        URI søknadURI = (getClass().getClassLoader().getResource("soknad_altinn.xml")).toURI();
        String xmlResponse = new String(Files.readAllBytes(Paths.get(søknadURI)));

        wireMockServer.stubFor(get(urlMatching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_XML_VALUE)
                .withBody(xmlResponse)
            )
        );

        MedlemskapArbeidEOSM søknad = soknadMottakConsumer.hentSøknad(søknadID);

        assertThat(søknad).isNotNull()
            .extracting(MedlemskapArbeidEOSM::getInnhold).isNotNull()
            .extracting(Innhold::getMidlertidigUtsendt).isNotNull()
            .extracting(MidlertidigUtsendt::getArbeidsland).isEqualTo(Landkoder.BG.getKode());

        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/soknader/" + søknadID))
                .withHeader("Accept", matching(MediaType.APPLICATION_XML_VALUE))
        );
    }

    @Test
    public void hentDokumenter_mottarListeAvDokumenter_blirMappet() throws JsonProcessingException {
        AltinnDokument altinnDokument = new AltinnDokument(
            søknadID, "dokID123", "tittel", "Fullmakt", "Base64EncodedPdf", Instant.MIN);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String jsonResponseBody = objectMapper.writeValueAsString(Collections.singleton(altinnDokument));

        wireMockServer.stubFor(get(urlMatching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(jsonResponseBody)
            )
        );

        Collection<AltinnDokument> dokumenter = soknadMottakConsumer.hentDokumenter(søknadID);

        assertThat(dokumenter)
            .isNotNull()
            .hasSize(1)
            .extracting(AltinnDokument::getTittel, AltinnDokument::getInnsendtTidspunkt)
            .containsExactly(tuple(altinnDokument.getTittel(), altinnDokument.getInnsendtTidspunkt()));

        wireMockServer.verify(
            getRequestedFor(urlPathEqualTo("/soknader/" + søknadID + "/dokumenter"))
                .withHeader("Accept", matching(MediaType.APPLICATION_JSON_VALUE))
        );
    }
}
