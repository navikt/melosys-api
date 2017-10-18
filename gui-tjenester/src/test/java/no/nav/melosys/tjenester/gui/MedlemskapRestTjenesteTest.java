package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.integrasjon.medl2.Medl2Fasade;
import no.nav.melosys.integrasjon.medl2.Medl2Service;
import no.nav.melosys.integrasjon.medl2.medlemskap.MedlemskapMock;
import no.nav.melosys.tjenester.gui.dto.MedlemsperiodeDto;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.*;

public class MedlemskapRestTjenesteTest {

    private Medl2Fasade medl2;

    private MedlemskapRestTjeneste restTjeneste;

    @Before
    public void setUp() {
        medl2 = new Medl2Service(new MedlemskapMock());

        restTjeneste = new MedlemskapRestTjeneste(medl2);
    }

    // FIXME: Reimplementer når EESSI2-335 er på plass
    @Ignore
    @Test
    @SuppressWarnings("unchecked")
    public void hentMedlemsperiodeListe() throws Exception {
        String fnr = "77777777778";
        Response response = restTjeneste.hentMedlemsperiodeListe(fnr);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JavaTimeModule());

        StringWriter writer = new StringWriter();
        objectMapper.writeValue(writer, response.getEntity());

        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof List);

        List<MedlemsperiodeDto> medlemsperiodeDtoListe = (List<MedlemsperiodeDto>) response.getEntity();

        assertNotNull(medlemsperiodeDtoListe);
        assertFalse(medlemsperiodeDtoListe.isEmpty());
    }
}
