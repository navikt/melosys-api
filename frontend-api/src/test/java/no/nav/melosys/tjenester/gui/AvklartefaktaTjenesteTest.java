package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(AvklartefaktaTjenesteTest.class);

    private static final String AVKLARTEFAKTA_SCHEMA = "avklartefakta-schema.json";

    private AvklartefaktaTjeneste avklartefaktaTjeneste;

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private TilgangService tilgangService;

    @Override
    public String schemaNavn() {
        return AVKLARTEFAKTA_SCHEMA;
    }

    @Before
    public void setUp() {
        avklartefaktaTjeneste = new AvklartefaktaTjeneste(avklartefaktaService, tilgangService);
    }

    @Test
    public void hentAvklartefakta() throws Exception {
        Set<AvklartefaktaDto> mockliste = defaultEasyRandom().objects(AvklartefaktaDto.class, 4).collect(Collectors.toSet());
        when(avklartefaktaService.hentAlleAvklarteFakta(1L)).thenReturn(mockliste);

        Set<AvklartefaktaDto> avklartefaktaDtoSet = avklartefaktaTjeneste.hentAvklarteFakta(1L);
        validerListe(avklartefaktaDtoSet);
    }

    @Test
    public void lagreAvklartefaktaGirKopiAvInput() throws Exception {
        Set<AvklartefaktaDto> avklartefaktaDtoer = defaultEasyRandom().objects(AvklartefaktaDto.class, 4).collect(Collectors.toSet());
        when(avklartefaktaService.hentAlleAvklarteFakta(1L)).thenReturn(avklartefaktaDtoer);
        Set<AvklartefaktaDto> resultat = avklartefaktaTjeneste.lagreAvklarteFakta(1, avklartefaktaDtoer);
        assertThat(resultat).isEqualTo(avklartefaktaDtoer);
    }

    @Test(expected = FunksjonellException.class)
    public void lagreAvklartefakta_ikkeRedigerbarBehandling_girFeil() throws FunksjonellException, TekniskException {
        doThrow(FunksjonellException.class).when(tilgangService).sjekkRedigerbarOgTilgang(anyLong());

        avklartefaktaTjeneste.lagreAvklarteFakta(1, Collections.emptySet());
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void hentAvklartefakta_ikkeTilgang_girFeil() throws FunksjonellException, TekniskException {
        doThrow(SikkerhetsbegrensningException.class).when(tilgangService).sjekkTilgang(anyLong());

        avklartefaktaTjeneste.hentAvklarteFakta(1);
    }
}

