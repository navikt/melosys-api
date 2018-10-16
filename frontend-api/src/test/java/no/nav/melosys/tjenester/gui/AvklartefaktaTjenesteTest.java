package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Set;

import no.nav.melosys.exception.IkkeFunnetException;
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

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaTjenesteTest extends JsonSchemaTest {

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
    public void hentAvklartefakta() throws IkkeFunnetException, IOException {
        Set<AvklartefaktaDto> mockliste = defaultEnhancedRandom().randomSetOf(4, AvklartefaktaDto.class);
        when(avklartefaktaService.hentAvklarteFakta(1L)).thenReturn(mockliste);

        Set<AvklartefaktaDto> avklartefaktaDtoSet = avklartefaktaTjeneste.hentAvklarteFakta(1L);
        validerListe(avklartefaktaDtoSet);
    }
}

