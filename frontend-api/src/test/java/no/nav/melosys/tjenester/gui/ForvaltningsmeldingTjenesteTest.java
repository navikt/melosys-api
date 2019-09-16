package no.nav.melosys.tjenester.gui;

import no.nav.melosys.service.ForvaltningsmeldingService;
import no.nav.melosys.service.abac.TilgangService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ForvaltningsmeldingTjenesteTest extends JsonSchemaTestParent {
    
    private ForvaltningsmeldingTjeneste tjeneste;

    @Mock
    private ForvaltningsmeldingService forvaltningsmeldingService;

    @Mock
    private TilgangService tilgangService;

    @Before
    public void setUp() {
        tjeneste = new ForvaltningsmeldingTjeneste(forvaltningsmeldingService, tilgangService);
    }

    @Test
    public void forvaltningsmelding_fungerer() throws Exception {
        long behandlingID = 3L;

        tjeneste.sendForvaltningsmelding(behandlingID);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(forvaltningsmeldingService).sendForvaltningsmelding(behandlingID);
    }
}

