package no.nav.melosys.tjenester.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.aareg.AaregService;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.ereg.EregService;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonMock;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.integrasjon.tps.person.PersonMock;
import no.nav.melosys.tjenester.gui.dto.view.ArbeidsforholdView;

// Tester json output til /arbeidsforhold
public class ArbeidsforholdRestTjenesteTest {

    private TpsFasade tps;

    private AaregFasade aareg;

    private EregFasade ereg;

    ArbeidsforholdRestTjeneste restTjeneste;

    @Before
    public void setUp() throws JAXBException {
        tps = new TpsService(null, new PersonMock());
        aareg = new AaregService(new ArbeidsforholdMock());
        ereg = new EregService(new OrganisasjonMock());

        restTjeneste = new ArbeidsforholdRestTjeneste(tps, aareg, ereg);
    }
    
    @Ignore
    @Test
    public void hentArbeidsforhold() throws Exception {
        String ident = "88888888884";

        Response response = restTjeneste.hentArbeidsforhold(ident);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JavaTimeModule());

        StringWriter writer = new StringWriter();
        objectMapper.writeValue(writer, response.getEntity());

        assertThat(response.getEntity()).isInstanceOf(ArbeidsforholdView.class);
        System.out.println(writer.toString());
    }

}