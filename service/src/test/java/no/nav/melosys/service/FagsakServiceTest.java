package no.nav.melosys.service;

import java.time.LocalDateTime;

import no.nav.melosys.domain.BehandlingType;
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
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class FagsakServiceTest {

    @Mock
    private FagsakRepository fagsakRepo;

    private SaksopplysningerService saksopplysningerService;

    private FagsakService fagsakService;

    @Before
    public void setUp() {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        TpsFasade tps = new TpsService(null, null, dokumentFactory, null);
        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        EregFasade ereg = new EregService(new OrganisasjonMock(), dokumentFactory);
        MedlFasade medl = new MedlService(new MedlemskapMock(), dokumentFactory);
        InntektFasade inntekt = new InntektService(new InntektMock(), dokumentFactory);

        saksopplysningerService = new SaksopplysningerService(tps, aareg, ereg, medl, inntekt);
        ReflectionTestUtils.setField(saksopplysningerService, "arbeidsforholdhistorikkAntallÅr", 5);
        ReflectionTestUtils.setField(saksopplysningerService, "inntektshistorikkAntallMåneder", 12);

        fagsakRepo = mock(FagsakRepository.class);
        BehandlingRepository behandlingRepo = mock(BehandlingRepository.class);
        fagsakService = new FagsakService(fagsakRepo, behandlingRepo, saksopplysningerService, tps);
    }

    @Test
    public void lagFagsak() throws Exception {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer("123");
        fagsak.setStatus(FagsakStatus.OPPRETTET);
        fagsak.setType(FagsakType.EU_EØS);
        fagsak.setRegistrertDato(LocalDateTime.now());

        fagsakService.lagre(fagsak);
        assertNotNull(fagsak);
        assertNotNull(fagsak.getSaksnummer());
    }

    @Test
    public void nyFagsak() throws Exception {
        final String[] identer = new String[]{"88888888884", "77777777779"};

        for (String fnr : identer) {
            Fagsak fagsak = fagsakService.nyFagsakOgBehandling(fnr, BehandlingType.SØKNAD);

            assertNotNull(fagsak);
            assertFalse(fagsak.getBehandlinger().isEmpty());

        }
    }
}