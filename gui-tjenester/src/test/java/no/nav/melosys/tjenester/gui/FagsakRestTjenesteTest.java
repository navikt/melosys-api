package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

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
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektMock;
import no.nav.melosys.integrasjon.medl.Medl2Fasade;
import no.nav.melosys.integrasjon.medl.Medl2Service;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.integrasjon.tps.person.PersonMock;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FagsakRestTjenesteTest {

    private FagsakRepository fagsakRepo;

    private FagsakRestTjeneste tjeneste;

    @Before
    public void setUp() throws JAXBException {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        TpsFasade tps = new TpsService(null, new PersonMock(), dokumentFactory);
        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        EregFasade ereg = new EregService(new OrganisasjonMock(), dokumentFactory);
        Medl2Fasade medl = new Medl2Service(new MedlemskapMock(), dokumentFactory);
        InntektFasade inntekt = new InntektService(new InntektMock(), dokumentFactory);

        fagsakRepo = Mockito.mock(FagsakRepository.class);
        FagsakService fagsakService = new FagsakService(fagsakRepo, tps, aareg, ereg, medl, inntekt);
        tjeneste = new FagsakRestTjeneste(fagsakService, dokumentFactory);
    }

    @Test
    public void hentFagsak() throws Exception {
        long saksnummer = 123;

        //Response response = tjeneste.hentFagsak(saksnummer);

    }

    @Test
    public void testMapping() throws Exception {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(123L);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        fagsak.setType(FagsakType.SØKNAD_A1);
        fagsak.setRegistrertDato(LocalDateTime.now());

        ModelMapper modelMapper = new ModelMapper();
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        modelMapper.validate();

        fagsakRepo.save(fagsak);

        //System.out.println("ID " + fagsak.getId());

    }

    @Test
    public void nyFagsak() {
        final String[] identer = new String[]{"88888888884", "77777777779"};

        for (String fnr : identer) {
            Response response = tjeneste.nyFagsak(fnr);

            assertTrue(response.getEntity() instanceof FagsakDto);

            FagsakDto fagsak = (FagsakDto) response.getEntity();

            assertNotNull(fagsak);
            assertFalse(fagsak.getBehandlinger().isEmpty());

            for (BehandlingDto behandling : fagsak.getBehandlinger()) {
                assertFalse(behandling.getSaksopplysninger().isEmpty());
            }

            //printJson(response);
        }
    }

    private void printJson(Response response) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.registerModule(new JavaTimeModule());

        StringWriter writer = new StringWriter();

        try {
            objectMapper.writeValue(writer, response.getEntity());
            System.out.println(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}