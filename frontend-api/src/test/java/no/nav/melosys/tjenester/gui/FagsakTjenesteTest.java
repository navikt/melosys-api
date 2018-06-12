package no.nav.melosys.tjenester.gui;

import java.time.LocalDateTime;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.aareg.AaregService;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.ereg.EregService;
import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonMock;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektMock;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.medl.MedlService;
import no.nav.melosys.integrasjon.medl.medlemskap.MedlemskapMock;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FagsakTjenesteTest {

    private FagsakRepository fagsakRepo;

    private FagsakTjeneste tjeneste;

    @Before
    public void setUp() throws JAXBException {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        TpsFasade tps = mock(TpsFasade.class);
        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        EregFasade ereg = new EregService(new OrganisasjonMock(), dokumentFactory);
        MedlFasade medl = new MedlService(new MedlemskapMock(), dokumentFactory);
        InntektFasade inntekt = new InntektService(new InntektMock(), dokumentFactory);

        fagsakRepo = mock(FagsakRepository.class);
        SaksopplysningerService saksopplysningerService = new SaksopplysningerService(tps, aareg, ereg, medl, inntekt);
        ReflectionTestUtils.setField(saksopplysningerService, "arbeidsforholdhistorikkAntallÅr", 5);
        ReflectionTestUtils.setField(saksopplysningerService, "inntektshistorikkAntallMåneder", 12);
        FagsakService fagsakService = new FagsakService(fagsakRepo, saksopplysningerService);
        tjeneste = new FagsakTjeneste(fagsakService);
    }

    @Test
    public void hentFagsak() throws Exception {
        // FIXME: Manglende test?
    }

    @Test
    public void testMapping() throws Exception {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer("123");
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        fagsak.setType(FagsakType.EU_EØS);
        fagsak.setRegistrertDato(LocalDateTime.now());

        ModelMapper modelMapper = new ModelMapper();
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        modelMapper.validate();

        fagsakRepo.save(fagsak);
    }

    @Test
    public void nyFagsak() {
        // Skru av logging for denne testen siden den skaper mye forventet støy
        final Logger log = (Logger) LoggerFactory.getLogger(SaksopplysningerService.class);
        Level opprinneligLevel = log.getLevel();
        log.setLevel(Level.OFF);

        final String[] identer = new String[]{"88888888884", "77777777779"};

        for (String fnr : identer) {
            Response response = tjeneste.nyFagsak(fnr);

            assertTrue(response.getEntity() instanceof FagsakDto);

            FagsakDto fagsak = (FagsakDto) response.getEntity();

            assertNotNull(fagsak);
            assertFalse(fagsak.getBehandlinger().isEmpty());

            response.close();
        }
        // Skru på logging igjen
        log.setLevel(opprinneligLevel);
    }

}