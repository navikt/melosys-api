package no.nav.melosys.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import no.nav.melosys.domain.Behandling;
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
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.integrasjon.tps.person.PersonMock;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class FagsakServiceTest {

    @Mock
    private FagsakRepository fagsakRepo;

    private FagsakService fagsakService;

    @Before
    public void setUp() {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        TpsFasade tps = new TpsService(null, new PersonMock(), dokumentFactory);
        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        EregFasade ereg = new EregService(new OrganisasjonMock(), dokumentFactory);
        MedlFasade medl = new MedlService(new MedlemskapMock(), dokumentFactory);
        InntektFasade inntekt = new InntektService(new InntektMock(), dokumentFactory);

        fagsakRepo = Mockito.mock(FagsakRepository.class);
        fagsakService = new FagsakService(fagsakRepo, tps, aareg, ereg, medl, inntekt);

        ReflectionTestUtils.setField(fagsakService, "arbeidsforholdhistorikkAntallMåneder", 12);
        ReflectionTestUtils.setField(fagsakService, "inntektshistorikkAntallMåneder", 12);
    }

    @Test
    public void lagFagsak() throws Exception {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(123L);
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        fagsak.setType(FagsakType.SØKNAD_A1);
        fagsak.setRegistrertDato(LocalDateTime.now());

        fagsakRepo.save(fagsak);

        assertNotNull(fagsak);
    }

    @Test
    public void nyFagsak() throws Exception {
        // Skru av logging for denne testen siden den skaper mye forventet støy
        final Logger log = (Logger) LoggerFactory.getLogger(FagsakService.class);
        Level opprinneligLevel = log.getLevel();
        log.setLevel(Level.OFF);

        final String[] identer = new String[]{"88888888884", "77777777779"};

        for (String fnr : identer) {
            Fagsak fagsak = fagsakService.nyFagsak(fnr);

            assertNotNull(fagsak);
            assertFalse(fagsak.getBehandlinger().isEmpty());

            for (Behandling behandling : fagsak.getBehandlinger()) {
                assertFalse(behandling.getSaksopplysninger().isEmpty());
            }

            //printJson(fagsak);
        }
        // Skru på logging igjen
        log.setLevel(opprinneligLevel);
    }

}