package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.integrasjon.dokgen.dto.SaksbehandlingstidSoknad;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokgenServiceTest {

    private final RestTemplate mockRestTemplate = mock(RestTemplate.class);

    private final DokgenService dokgenService = new DokgenService(new DokgenConsumer(mockRestTemplate));

    @Test
    @Disabled //FIXME
    void lagPdf() throws Exception {
        byte[] pdf = "pdf".getBytes();
        when(mockRestTemplate.postForObject(anyString(), any(HttpEntity.class), eq(byte[].class))).thenReturn(pdf);

        Behandling behandling = new Behandling();
        byte[] pdfResponse = dokgenService.lagPdf("test-mal", SaksbehandlingstidSoknad.map(behandling));
        assertNotNull(pdfResponse);
        assertEquals(pdf, pdfResponse);
    }

}