package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Map;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.statistikk.StatistikkService;
import no.nav.melosys.tjenester.gui.dto.statistikk.StatistikkDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatistikkTjenesteTest extends JsonSchemaTestParent {
    private static final String STATISTIKK_SCHEMA = "statistikk-schema.json";

    @Mock
    StatistikkService statistikkService;

    @Test
    void hentStatistikk_schemaValidert() throws IOException {
        StatistikkTjeneste statistikkTjeneste = new StatistikkTjeneste(statistikkService);
        when(statistikkService.hentBehandlingstatistikk()).thenReturn(lagBehandlingStatistikk());

        ResponseEntity<StatistikkDto> statistikkDtoResponseEntity = statistikkTjeneste.hentStatistikk();

        StatistikkDto statistikkDto = statistikkDtoResponseEntity.getBody();
        valider(statistikkDto, STATISTIKK_SCHEMA);
    }

    private Map<Behandlingstema, Long> lagBehandlingStatistikk() {
        return Map.of(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL, 2L, Behandlingstema.TRYGDETID, 99L);
    }
}
