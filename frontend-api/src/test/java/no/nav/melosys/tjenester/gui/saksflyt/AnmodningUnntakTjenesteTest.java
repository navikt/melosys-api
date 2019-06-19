package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;

import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
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
    private Tilgang tilgang;

    private AnmodningUnntakTjeneste anmodningUnntakTjeneste;

    @Override
    public String schemaNavn() {
        return "saksflyt-vedtak-post-schema.json";
    }

    @Before
    public void setUp() {
        anmodningUnntakTjeneste = new AnmodningUnntakTjeneste(anmodningUnntakService, tilgang);
    }

    @Test
    public void anmodningOmUnntak_fungerer() throws FunksjonellException, TekniskException, IOException {
        FattVedtakDto fattVedtakDto = new FattVedtakDto();
        long behandlingID = 3;
        fattVedtakDto.setBehandlingsresultattype(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        anmodningUnntakTjeneste.anmodningOmUnntak(behandlingID, fattVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        verify(anmodningUnntakService).anmodningOmUnntak(behandlingID);

        valider(fattVedtakDto);
    }
}