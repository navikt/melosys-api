package no.nav.melosys.tjenester.gui.saksflyt;

import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.AnmodningUnntakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnmodningUnntakTjenesteTest extends JsonSchemaTestParent {

    private static final String ANMODNING_UNNTAK_POST_SCHEMA = "saksflyt-anmodningsperioder-bestill-post-schema.json";
    @Mock
    private AnmodningUnntakService anmodningUnntakService;
    @Mock
    private TilgangService tilgangService;

    private AnmodningUnntakTjeneste anmodningUnntakTjeneste;

    @BeforeEach
    public void setUp() {
        anmodningUnntakTjeneste = new AnmodningUnntakTjeneste(anmodningUnntakService, tilgangService);
    }

    @Test
    void anmodningOmUnntak_fungerer() throws Exception {
        final long behandlingID = 3;
        final String mottakerInstitusjon = "SE:321";
        final String fritekstSed = "hei hei";

        AnmodningUnntakDto dto = new AnmodningUnntakDto();
        dto.setMottakerinstitusjon(mottakerInstitusjon);
        dto.setFritekstSed(fritekstSed);
        anmodningUnntakTjeneste.anmodningOmUnntak(behandlingID, dto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(anmodningUnntakService).anmodningOmUnntak(
            eq(behandlingID), eq(mottakerInstitusjon), eq(fritekstSed), anyCollection());

        valider(dto, ANMODNING_UNNTAK_POST_SCHEMA);
    }
}