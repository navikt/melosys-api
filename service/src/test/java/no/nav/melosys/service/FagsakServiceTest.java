package no.nav.melosys.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
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
import no.nav.melosys.integrasjon.tps.aktoer.AktoerIdCache;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FagsakServiceTest {

    @Mock
    private FagsakRepository fagsakRepo;

    private FagsakService fagsakService;

    private ProsessinstansRepository prosessinstansRepository;

    private BehandlingRepository behandlingRepo;

    private AktoerIdCache aktoerIdCache;

    private Binge binge;

    @Before
    public void setUp() {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());
        aktoerIdCache = mock(AktoerIdCache.class);
        TpsFasade tps = new TpsService(null, null, dokumentFactory, aktoerIdCache);
        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        EregFasade ereg = new EregService(new OrganisasjonMock(), dokumentFactory);
        MedlFasade medl = new MedlService(new MedlemskapMock(), dokumentFactory);
        InntektFasade inntekt = new InntektService(new InntektMock(), dokumentFactory);
        prosessinstansRepository = mock(ProsessinstansRepository.class );

        SaksopplysningerService saksopplysningerService = new SaksopplysningerService(tps, aareg, ereg, medl, inntekt);
        ReflectionTestUtils.setField(saksopplysningerService, "arbeidsforholdhistorikkAntallÅr", 5);
        ReflectionTestUtils.setField(saksopplysningerService, "inntektshistorikkAntallMåneder", 12);

        fagsakRepo = mock(FagsakRepository.class);
        behandlingRepo = mock(BehandlingRepository.class);
        binge = new BingeTestImpl();
        fagsakService = new FagsakService(fagsakRepo, behandlingRepo, saksopplysningerService, tps, prosessinstansRepository, binge);
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
    public void oppfriskSaksopplysning() throws IkkeFunnetException {

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId("123");
        aktoer.setRolle(RolleType.BRUKER);
        HashSet<Aktoer> aktoers = new HashSet<>();
        aktoers.add(aktoer);
        fagsak.setAktører(aktoers);
        behandling.setFagsak(fagsak);

        HashSet<Saksopplysning> saksopplysnings = new HashSet<>();

        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysnings.add(saksopplysningPerson);

        SoeknadDokument soeknadDokument = new SoeknadDokument();

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.arbeidsland = new ArrayList<>();
        arbeidUtland.arbeidsland.add(new Land(Land.NORGE));
        arbeidUtland.arbeidsperiode = new Periode(LocalDate.now(),LocalDate.of(2018,9,18));
        soeknadDokument.arbeidUtland = arbeidUtland;

        Saksopplysning saksopplysningSøknad = new Saksopplysning();
        saksopplysningSøknad.setType(SaksopplysningType.SØKNAD);
        saksopplysningSøknad.setDokument(soeknadDokument);
        saksopplysnings.add(saksopplysningSøknad);

        behandling.setSaksopplysninger(saksopplysnings);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        ArrayList<Prosessinstans> prosessinstansArrayList = new ArrayList<>();
        prosessinstansArrayList.add(prosessinstans);

         when(prosessinstansRepository.findByBehandling_Id(anyLong())).thenReturn(prosessinstansArrayList);
         when(behandlingRepo.findOne(anyLong())).thenReturn(behandling);
         when(aktoerIdCache.hentIdentFraCache(any())).thenReturn("123456");

        fagsakService.oppfriskSaksopplysning(anyLong());

        assertThat(prosessinstans.getBehandling().getSaksopplysninger().size()).isEqualTo(1);
        assertThat(prosessinstans.getBehandling().getSaksopplysninger().stream().findFirst().get().getType()).isEqualTo(SaksopplysningType.SØKNAD);
        assertThat(binge.hentProsessinstans(prosessinstans.getId()).getSteg()).isEqualTo(ProsessSteg.JFR_HENT_PERS_OPPL);
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