package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VilkaarTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(VilkaarTjenesteTest.class);

    private static final String VILKÅR_SCHEMA = "vilkaar-schema.json";

    @Mock
    private VilkaarsresultatService vilkaarsresultatService;
    @Mock
    private InngangsvilkaarService inngangsvilkaarService;
    @Mock
    private Aksesskontroll aksesskontroll;

    private VilkaarTjeneste vilkaarTjeneste;

    @BeforeEach
    public void setUp() {
        vilkaarTjeneste = new VilkaarTjeneste(vilkaarsresultatService, inngangsvilkaarService, aksesskontroll);
    }

    @Test
    void hentVilkår_validerSchema() throws Exception {
        List<VilkaarDto> mockListe = defaultEasyRandom().objects(VilkaarDto.class, 4).collect(Collectors.toList());
        mockListe.forEach(v -> v.setVilkaar(Vilkaar.FTRL_2_8_FORUTGÅENDE_TRYGDETID.getKode()));
        when(vilkaarsresultatService.hentVilkaar(1L)).thenReturn(mockListe);

        List<VilkaarDto> vilkaarDtoListe = vilkaarTjeneste.hentVilkår(1L);

        validerArray(vilkaarDtoListe, VILKÅR_SCHEMA, log);
    }
}
