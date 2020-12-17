package no.nav.melosys.statistikk.utstedt_a1.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
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
class UtstedtA1AdminTjenesteTest {

    @Mock
    private UtstedtA1Service utstedtA1Service;
    @Mock
    private BehandlingRepository behandlingRepository;
    private final String apiKey = "dummy";

    private UtstedtA1AdminTjeneste utstedtA1AdminTjeneste;

    @BeforeEach
    void setUp() {
        utstedtA1AdminTjeneste = new UtstedtA1AdminTjeneste(utstedtA1Service, behandlingRepository, apiKey);
    }

    @Test
    void publiser() throws Exception {
        utstedtA1AdminTjeneste.publiserMelding(apiKey, 1L);
        verify(utstedtA1Service).sendMeldingOmUtstedtA1(eq(1L));
    }

    @Test
    void publiserEksisterendeBehandlinger_medIngenOppgitteBehandlinger_forventListe() throws Exception {
        when(behandlingRepository.findAll()).thenReturn(List.of(
            lagBehandling(1L),
            lagBehandling(2L),
            lagBehandling(3L)
        ));

        Map<String, Set<Long>> behandlinger = utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger(apiKey, Collections.emptySet()).getBody();

        assertThat(behandlinger).isNotNull();
        assertThat(behandlinger.get("feiledeBehandlinger")).isEmpty();
        assertThat(behandlinger.get("sendteBehandlinger")).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void publiserEksisterendeBehandlinger_medOppgitteBehandlingerIngenFeiledeBehandlinger_forventListe() throws Exception {
        when(behandlingRepository.findAllById(anyIterable())).thenReturn(List.of(
            lagBehandling(1L),
            lagBehandling(2L),
            lagBehandling(3L)
        ));

        Map<String, Set<Long>> behandlinger = utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger(apiKey, Set.of(1L, 2L, 3L)).getBody();

        assertThat(behandlinger).isNotNull();
        assertThat(behandlinger.get("feiledeBehandlinger")).isEmpty();
        assertThat(behandlinger.get("sendteBehandlinger")).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void publiserEksisterendeBehandlinger_medOppgitteBehandlingerOgBehandlingFeiler_forventListe() throws Exception {
        when(behandlingRepository.findAllById(anyIterable())).thenReturn(List.of(
            lagBehandling(1L),
            lagBehandling(2L),
            lagBehandling(3L)
        ));
        doNothing().when(utstedtA1Service).sendMeldingOmUtstedtA1(or(eq(1L), eq(2L)));
        doThrow(new TekniskException("ugyldig behandling")).when(utstedtA1Service).sendMeldingOmUtstedtA1(3L);

        Map<String, Set<Long>> behandlinger = utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger(apiKey, Set.of(1L, 2L, 3L)).getBody();

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
            .isThrownBy(() -> utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger("Dumdum", Collections.emptySet()))
            .withMessageContaining("apikey");
    }

    private Behandling lagBehandling(Long behandlingID) {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        return behandling;
    }
}