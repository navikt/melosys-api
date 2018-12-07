package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.TidligereMedlemsperioderDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(BehandlingTjenesteTest.class);

    private static final String TIDLIGERE_MEDLEMSPERIODER_SCHEMA = "behandlinger-perioder-post-schema.json";

    private BehandlingTjeneste behandlingTjeneste;

    @Mock
    private BehandlingService behandlingService;

    @Before
    public void setUp() {
        behandlingTjeneste = new BehandlingTjeneste(behandlingService, mock(Tilgang.class));
    }

    @Override
    public String schemaNavn() {
        return TIDLIGERE_MEDLEMSPERIODER_SCHEMA;
    }

    @Test
    public void behandlingerPerioderValidering() throws IOException {
        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = Arrays.asList(2L, 3L, 5L);

        String jsonString = objectMapper().writeValueAsString(tidligereMedlemsperioderDto);
        assertThat(jsonString).isNotEmpty();
        valider(jsonString, log);
    }

    @Test
    public void knyttMedlemsperioder() throws Exception {
        long behandlingID = 11L;
        List<Long> periodeIder = Arrays.asList(2L, 3L, 5L);
        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = periodeIder;

        behandlingTjeneste.knyttMedlemsperioder(behandlingID, tidligereMedlemsperioderDto);
        verify(behandlingService, times(1)).knyttMedlemsperioder(behandlingID, periodeIder);
    }

    @Test
    public void hentMedlemsperioder() throws Exception {
        long behandlingID = 11L;
        List<Long> periodeIder = Arrays.asList(2L, 3L, 5L);
        when(behandlingService.hentMedlemsperioder(behandlingID)).thenReturn(periodeIder);

        Response response = behandlingTjeneste.hentMedlemsperioder(behandlingID);
        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getEntity()).isInstanceOf(TidligereMedlemsperioderDto.class);

        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = (TidligereMedlemsperioderDto) response.getEntity();
        assertThat(tidligereMedlemsperioderDto.periodeIder).containsAll(periodeIder);

        verify(behandlingService, times(1)).hentMedlemsperioder(behandlingID);
    }
}
