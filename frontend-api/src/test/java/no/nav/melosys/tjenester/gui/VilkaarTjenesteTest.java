package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.List;

import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VilkaarTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(VilkaarTjenesteTest.class);

    private static final String VILKÅR_SCHEMA = "vilkaar-schema.json";

    @Mock
    private VilkaarsresultatService vilkaarsresultatService;

    @Mock
    private Tilgang tilgang;

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
        vilkaarTjeneste = new VilkaarTjeneste(vilkaarsresultatService, tilgang);
    }

    @Test
    @Ignore //FIXME Definere schema
    public void hentVilkår() throws IOException {
        List<VilkaarDto> mockListe = defaultEnhancedRandom().randomListOf(4, VilkaarDto.class);
        when(vilkaarsresultatService.hentVilkaar(1L)).thenReturn(mockListe);

        List<VilkaarDto> vilkaarDtoListe = vilkaarTjeneste.hentVilkår(1L);

        validerListe(vilkaarDtoListe);
    }
}