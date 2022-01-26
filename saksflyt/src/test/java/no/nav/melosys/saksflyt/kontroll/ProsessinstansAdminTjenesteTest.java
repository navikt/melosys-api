package no.nav.melosys.saksflyt.kontroll;

import java.util.List;

import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.saksflyt.kontroll.dto.RestartProsessinstanserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static reactor.core.publisher.Mono.when;

@ExtendWith(MockitoExtension.class)
public class ProsessinstansAdminTjenesteTest {

    @Mock
    private ProsessinstansAdminService mockProsessinstansAdminService;
    private final String API_KEY = "dummy";

    private ProsessinstansAdminTjeneste prosessinstansAdminTjeneste;

    @BeforeEach
    public void setup() {
        prosessinstansAdminTjeneste = new ProsessinstansAdminTjeneste(mockProsessinstansAdminService, API_KEY);
    }

    @Test
    void restartProssessinstans_riktigApiKeyOppgitt_forventOk() {
        var response = prosessinstansAdminTjeneste.restartAlleFeiledeProsessinstanser(API_KEY);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void hentFeiledeProsessinstanser_feilApiKeyOppgitt_forventForbidden() {
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> prosessinstansAdminTjeneste.hentFeiledeProsessinstanser("Dum dummy"))
            .withMessageContaining("apikey");
    }

    @Test
    void restartProsessinstans_feilApiKeyOppgitt_forventForbidden() {
        final var request = new RestartProsessinstanserRequest(List.of());
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> prosessinstansAdminTjeneste.restartProsessinstans("Dumdum", request))
            .withMessageContaining("apikey");
    }
}
