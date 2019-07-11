package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VilkaarTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(VilkaarTjenesteTest.class);

    private static final String VILKÅR_SCHEMA = "vilkar-schema.json";

    @Mock
    private VilkaarsresultatService vilkaarsresultatService;

    @Mock
    private TilgangService tilgangService;

    private VilkaarTjeneste vilkaarTjeneste;

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String schemaNavn() {
        return VILKÅR_SCHEMA;
    }

    @Before
    public void setUp() {
        vilkaarTjeneste = new VilkaarTjeneste(vilkaarsresultatService, tilgangService);
    }

    @Test
    public void hentVilkår() throws Exception {
        List<VilkaarDto> mockListe = defaultEasyRandom().objects(VilkaarDto.class, 4).collect(Collectors.toList());
        when(vilkaarsresultatService.hentVilkaar(1L)).thenReturn(mockListe);

        List<VilkaarDto> vilkaarDtoListe = vilkaarTjeneste.hentVilkår(1L);

        validerListe(vilkaarDtoListe);
    }

    @Test(expected = FunksjonellException.class)
    public void lagreVilkaar_ikkeRedigerbarBehandling_girFeil() throws FunksjonellException, TekniskException {
        doThrow(FunksjonellException.class).when(tilgangService).sjekkRedigerbarOgTilgang(anyLong());

        vilkaarTjeneste.registrerVilkår(1L, Collections.emptyList());
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void hentVilkaar_ikkeTilgang_girFeil() throws FunksjonellException, TekniskException {
        doThrow(SikkerhetsbegrensningException.class).when(tilgangService).sjekkTilgang(anyLong());

        vilkaarTjeneste.hentVilkår(1L);
    }
}