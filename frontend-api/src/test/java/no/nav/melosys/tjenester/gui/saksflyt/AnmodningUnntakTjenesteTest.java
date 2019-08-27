package no.nav.melosys.tjenester.gui.saksflyt;

import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakTjenesteTest {
    @Mock
    private AnmodningUnntakService anmodningUnntakService;
    @Mock
    private TilgangService tilgangService;
    @Mock
    private BehandlingRepository behandlingRepository;

    private AnmodningUnntakTjeneste anmodningUnntakTjeneste;

    @Before
    public void setUp() {
        anmodningUnntakTjeneste = new AnmodningUnntakTjeneste(anmodningUnntakService, tilgangService, behandlingRepository);
    }

    @Test
    public void anmodningOmUnntak_fungerer() throws Exception {
        long behandlingID = 3;

        anmodningUnntakTjeneste.anmodningOmUnntak(behandlingID);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(anmodningUnntakService).anmodningOmUnntak(behandlingID);
    }
}