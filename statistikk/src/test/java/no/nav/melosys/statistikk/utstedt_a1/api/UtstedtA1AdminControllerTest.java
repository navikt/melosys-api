package no.nav.melosys.statistikk.utstedt_a1.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VedtakMetadataRepository;
import no.nav.melosys.statistikk.utstedt_a1.service.UtstedtA1Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtstedtA1AdminControllerTest {

    @Mock
    private UtstedtA1Service utstedtA1Service;
    @Mock
    private VedtakMetadataRepository vedtakMetadataRepository;
    private final String apiKey = "dummy";

    private UtstedtA1AdminController utstedtA1AdminTjeneste;

    @BeforeEach
    void setUp() {
        utstedtA1AdminTjeneste = new UtstedtA1AdminController(utstedtA1Service, vedtakMetadataRepository, apiKey);
    }

    @Test
    void publiser() {
        utstedtA1AdminTjeneste.publiserMelding(apiKey, 1L);
        verify(utstedtA1Service).sendMeldingOmUtstedtA1(eq(1L));
    }

    @Test
    void publiserEksisterendeBehandlinger_forventListe() {
        when(vedtakMetadataRepository.findBehandlingsresultatIdByRegistrertDatoIsGreaterThanEqual(anyInstant()))
            .thenReturn(List.of(1L, 2L, 3L));

        Map<String, Set<Long>> behandlinger = utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger(apiKey, LocalDate.now()).getBody();

        assertThat(behandlinger).isNotNull();
        assertThat(behandlinger.get("feiledeBehandlinger")).isEmpty();
        assertThat(behandlinger.get("sendteBehandlinger")).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void publiserEksisterendeBehandlinger_medOppgitteBehandlingerOgBehandlingFeiler_forventListe() {
        when(vedtakMetadataRepository.findBehandlingsresultatIdByRegistrertDatoIsGreaterThanEqual(anyInstant()))
            .thenReturn(List.of(1L, 2L, 3L));
        doNothing().when(utstedtA1Service).sendMeldingOmUtstedtA1(or(eq(1L), eq(2L)));
        doThrow(new TekniskException("ugyldig behandling")).when(utstedtA1Service).sendMeldingOmUtstedtA1(3L);

        Map<String, Set<Long>> behandlinger = utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger(apiKey, LocalDate.now()).getBody();

        assertThat(behandlinger).isNotNull();
        assertThat(behandlinger.get("feiledeBehandlinger")).containsExactly(3L);
        assertThat(behandlinger.get("sendteBehandlinger")).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void feilApiKeyOppgittForventForbidden() {
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> utstedtA1AdminTjeneste.publiserMelding("Dum dummy", 1L))
            .withMessageContaining("apikey");
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger("Dumdum", LocalDate.now()))
            .withMessageContaining("apikey");
    }

    private static Instant anyInstant() {
        return any(Instant.class);
    }
}
