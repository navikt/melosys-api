package no.nav.melosys.tjenester.gui.saksflyt;

import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.AnmodningUnntakDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakTjenesteTest extends JsonSchemaTestParent {

    private static final String ANMODNING_UNNTAK_POST_SCHEMA = "saksflyt-anmodningsperioder-post-schema.json";
    @Mock
    private AnmodningUnntakService anmodningUnntakService;
    @Mock
    private TilgangService tilgangService;

    private AnmodningUnntakTjeneste anmodningUnntakTjeneste;

    @Before
    public void setUp() {
        anmodningUnntakTjeneste = new AnmodningUnntakTjeneste(anmodningUnntakService, tilgangService);
    }

    @Test
    public void anmodningOmUnntak_fungerer() throws Exception {
        final long behandlingID = 3;
        final String mottakerInstitusjon = "SE:321";

        AnmodningUnntakDto dto = new AnmodningUnntakDto();
        dto.setMottakerInstitusjon(mottakerInstitusjon);
        anmodningUnntakTjeneste.anmodningOmUnntak(behandlingID, dto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(anmodningUnntakService).anmodningOmUnntak(eq(behandlingID), eq(mottakerInstitusjon));

        valider(dto, ANMODNING_UNNTAK_POST_SCHEMA);
    }
}