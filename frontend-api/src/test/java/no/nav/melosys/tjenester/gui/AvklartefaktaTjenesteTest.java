package no.nav.melosys.tjenester.gui;

import java.util.Arrays;

import no.nav.melosys.domain.Avklartefakta;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.melosys.service.abac.Tilgang;


import static org.mockito.Mockito.mock;

public class AvklartefaktaTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(AvklartefaktaTjenesteTest.class);

    @InjectMocks
    private AvklarteFaktaTjeneste avklartefaktaTjeneste;

    @Mock
    private BehandlingService behandlingService;

    @Override
    public String schemaNavn() {
        return "soknad-schema.json";
    }

    @Before
    public void setUp()  {

        Tilgang tilgang = mock(Tilgang.class);
    }

    @Captor ArgumentCaptor<Avklartefakta> avklartefakta;
    @Test
    public void testPostBostedAvklaring() {
        String[] bostedLand = {"NO"};
        String begrunnelse = "Familie";

        AvklartefaktaDto avklartefaktaDto = mock(AvklartefaktaDto.class);
        avklartefaktaDto.setFakta(Arrays.asList(bostedLand));
        avklartefaktaDto.setBegrunnelsefritekst(begrunnelse);

        //avklartefaktaTjeneste.postBostedAvklaring(1234567, bosted);

        //verify(behandlingService).lagreAvklarteFakta(avklartefakta.capture());
        //assertEquals(avklartefakta.getValue().getBostedsland(),bostedLand);
    }

    @Test
    public void avklartefaktaSchemaValidering()  {

    }
}

