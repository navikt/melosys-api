package no.nav.melosys.tjenester.gui.saksflyt;

import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakTjenesteTest extends JsonSchemaTestParent {
    @Mock
    private AnmodningUnntakService anmodningUnntakService;
    @Mock
    private TilgangService tilgangService;

    private AnmodningUnntakTjeneste anmodningUnntakTjeneste;

    @Override
    public String schemaNavn() {
        return "saksflyt-vedtak-post-schema.json";
    }

    @Before
    public void setUp() {
        anmodningUnntakTjeneste = new AnmodningUnntakTjeneste(anmodningUnntakService, tilgangService);
    }

    @Test
    public void anmodningOmUnntak_fungerer() throws Exception {
        FattVedtakDto fattVedtakDto = new FattVedtakDto();
        long behandlingID = 3;
        fattVedtakDto.setBehandlingsresultattype(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        anmodningUnntakTjeneste.anmodningOmUnntak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(anmodningUnntakService).anmodningOmUnntak(behandlingID);

        valider(fattVedtakDto);
    }
}