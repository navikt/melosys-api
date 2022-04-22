package no.nav.melosys.saksflyt.kontroll;

import java.util.List;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProsessinstansAdminTjenesteTest {

    @Mock
    private ProsessinstansAdminService mockProsessinstansAdminService;
    private final String API_KEY = "dummy";
    private final UUID UUID = new UUID(0, 0);

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

    @Test
    void hoppOverStegProsessinstans_riktigApiKeyOppgitt_ok() {
        final var nyttSteg = ProsessSteg.AVSLUTT_SAK_OG_BEHANDLING;
        when(mockProsessinstansAdminService.hoppOverStegProsessinstans(UUID)).thenReturn(nyttSteg);

        var response = prosessinstansAdminTjeneste.hoppOverStegStegProsessinstans(API_KEY, UUID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("SIST_FULLFORTE_STEG for prosessinstans 00000000-0000-0000-0000-000000000000 satt til AVSLUTT_SAK_OG_BEHANDLING og prosessinstans restartet");
    }

    @Test
    void hoppOverStegProsessinstans_feilApiKeyOppgitt_forbidden() {
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> prosessinstansAdminTjeneste.hoppOverStegStegProsessinstans("dumdum", UUID))
            .withMessageContaining("apikey");
    }
}
