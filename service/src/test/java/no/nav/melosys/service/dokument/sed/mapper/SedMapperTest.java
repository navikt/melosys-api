package no.nav.melosys.service.dokument.sed.mapper;

import java.io.IOException;
import java.net.URISyntaxException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.eux.model.medlemskap.impl.MedlemskapA009;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.sed.AbstraktSedData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class SedMapperTest {

    protected class SedMapperImpl implements SedMapper<MedlemskapA009, SedDataImpl> {

        @Override
        public MedlemskapA009 hentMedlemskap(SedDataImpl sedData) {
            return new MedlemskapA009();
        }

        @Override
        public SedType getSedType() {
            return SedType.A009;
        }
    }

    private final class SedDataImpl extends AbstraktSedData {}

    private SedMapperImpl sedMapper = new SedMapperImpl();
    private SedDataImpl sedData;
    private ObjectMapper objectMapper;

    @Before
    public void setup() throws IOException, URISyntaxException {
        sedData = SedDataStub.hent(new SedDataImpl());
        objectMapper = new ObjectMapper();
    }

    @Test
    public void hentNavObjektTest() throws TekniskException, IOException, FunksjonellException {
        JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(sedMapper.mapTilSed(sedData)));
        assertNotNull(root);
        assertEquals(sedMapper.getSedType().name(), root.get("sed").textValue());

        JsonNode nav = root.get("nav");
        assertNotNull(nav);

        JsonNode bruker = nav.get("bruker");
        assertNotNull(bruker);

        JsonNode person = bruker.get("person");
        assertEquals("Ola", person.get("fornavn").textValue());
        assertEquals("K", person.get("kjoenn").textValue());

    }
}
