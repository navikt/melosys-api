package no.nav.melosys.tjenester.gui;

import java.util.Set;

import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.service.abac.Tilgang;
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
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(AvklartefaktaTjenesteTest.class);

    private static final String AVKLARTEFAKTA_SCHEMA = "avklartefakta-schema.json";

    private AvklartefaktaTjeneste avklartefaktaTjeneste;

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private Tilgang tilgang;

    @Override
    public String schemaNavn() {
        return AVKLARTEFAKTA_SCHEMA;
    }

    @Before
    public void setUp() {
        avklartefaktaTjeneste = new AvklartefaktaTjeneste(avklartefaktaService, tilgang);
    }

    @Test
    public void hentAvklartefakta() throws Exception {
        Set<AvklartefaktaDto> mockliste = defaultEnhancedRandom().randomSetOf(4, AvklartefaktaDto.class);
        when(avklartefaktaService.hentAlleAvklarteFakta(1L)).thenReturn(mockliste);

        Set<AvklartefaktaDto> avklartefaktaDtoSet = avklartefaktaTjeneste.hentAvklarteFakta(1L);
        validerListe(avklartefaktaDtoSet);
    }

    @Test
    public void lagreAvklartefaktaGirKopiAvInput() throws Exception {
        Set<AvklartefaktaDto> avklartefaktaDtoer = EnhancedRandom.randomSetOf(4, AvklartefaktaDto.class);
        when(avklartefaktaService.hentAlleAvklarteFakta(1L)).thenReturn(avklartefaktaDtoer);
        Set<AvklartefaktaDto> resultat = avklartefaktaTjeneste.lagraAvklarteFakta(1, avklartefaktaDtoer);
        assertThat(resultat).isEqualTo(avklartefaktaDtoer);
    }
}

