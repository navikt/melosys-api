package no.nav.melosys.tjenester.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.tjenester.gui.dto.view.ArbeidsforholdView;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;

// Tester json output til /arbeidsforhold
public class ArbeidsforholdRestTjenesteTest {

    @Mock
    private TpsFasade tps;

    @Mock
    private AaregFasade aareg;

    @Mock
    private EregFasade ereg;

    ArbeidsforholdRestTjeneste restTjeneste;

    @Before
    public void setUp() throws JAXBException {
        tps = mock(TpsFasade.class);
        aareg = mock(AaregFasade.class);
        ereg = mock(EregFasade.class);

        restTjeneste = new ArbeidsforholdRestTjeneste(tps, aareg, ereg);
    }

    @Test
    public void hentArbeidsforhold() throws Exception {
        String ident = "88888888884";

        HentPersonResponse tpsSvar = new MockTjenesterSvarTps().SVAR;
        List<Arbeidsforhold> aaregSvar = new MockTjenesteSvarAareg().SVAR;
        Organisasjon eregSvar = new MockTjenesteSvarEreg().SVAR;

        when(tps.hentPersonMedAdresse(ident)).thenReturn(tpsSvar);
        when(aareg.finnArbeidsforholdPrArbeidstaker(ident, AaregFasade.REGELVERK_A_ORDNINGEN)).thenReturn(aaregSvar);
        when(ereg.hentOrganisasjon(any())).thenReturn(eregSvar);

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