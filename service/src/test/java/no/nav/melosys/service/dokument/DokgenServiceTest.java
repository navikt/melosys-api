package no.nav.melosys.service.dokument;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.dokgen.DokgenMalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DokgenServiceTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    private DokgenService dokgenService;

    private byte[] expectedPdf = "pdf".getBytes();

    @BeforeEach
    void init() {
        WebClient webClient = WebClient.builder()
            .exchangeFunction(clientRequest ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                    .body(String.valueOf(expectedPdf))
                    .build())
            ).build();

        dokgenService = new DokgenService(new DokgenConsumer(), new DokgenMalMapper(new FakeUnleash()));
    }

//    @Test
//    @Disabled //FIXME
//    void lagPdf() {
//        SaksbehandlingstidSoknad saksbehandlingstidSoknad = new SaksbehandlingstidSoknad("11223366554", "MEL-123", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), "Donald Duck",
//            "Donald Duck", asList("Andebygata 1"), "9999", "Andeby", Sakstyper.EU_EOS, Aktoersroller.BRUKER, false, null, null);
//
//
//        byte[] pdfResponse = dokgenService.lagPdf("test-mal", saksbehandlingstidSoknad);
//        assertNotNull(pdfResponse);
//        assertEquals(expectedPdf, pdfResponse);
//    }

}