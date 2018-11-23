package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.aareg.AaregService;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdMock;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class SaksopplysningerServiceTest {

    private SaksopplysningerService saksopplysningerService;

    private TpsFasade tpsFasade;

    private ProsessinstansRepository prosessinstansRepository;

    private BehandlingRepository behandlingRepo;

    private BehandlingsresultatService behandlingsresultatService;

    private Binge binge;

    @Before
    public void setUp() {
        DokumentFactory dokumentFactory = new DokumentFactory(new JaxbConfig().jaxb2Marshaller(), new XsltTemplatesFactory());

        AaregFasade aareg = new AaregService(new ArbeidsforholdMock(), dokumentFactory);
        behandlingRepo = mock(BehandlingRepository.class);
        binge = mock(Binge.class);
        prosessinstansRepository = mock(ProsessinstansRepository.class);
        tpsFasade = mock(TpsFasade.class);
        behandlingsresultatService = mock(BehandlingsresultatService.class);
        saksopplysningerService = new SaksopplysningerService(tpsFasade, aareg, prosessinstansRepository, binge, behandlingRepo, behandlingsresultatService);

        ReflectionTestUtils.setField(saksopplysningerService, "arbeidsforholdhistorikkAntallMåneder", 6);
        ReflectionTestUtils.setField(saksopplysningerService, "inntektshistorikkAntallMåneder", 6);
        ReflectionTestUtils.setField(saksopplysningerService, "medlemskaphistorikkAntallÅr", 5);
    }

    @Test
    public void hentArbeidsforholdHistorikk() throws MelosysException {
        final Long arbeidsforholdsID = 12608035L;
        ArbeidsforholdDokument dokument = saksopplysningerService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        assertFalse(dokument.getArbeidsforhold().isEmpty());
        assertTrue(dokument.getArbeidsforhold().get(0).getArbeidsavtaler().size() > 1);
    }

    @Test
    public void oppfriskSaksopplysning() throws IkkeFunnetException, TekniskException {

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        Aktoer aktør = new Aktoer();
        aktør.setAktørId("123");
        aktør.setRolle(RolleType.BRUKER);
        HashSet<Aktoer> aktører = new HashSet<>();
        aktører.add(aktør);
        fagsak.setAktører(aktører);
        behandling.setFagsak(fagsak);

        HashSet<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysninger.add(saksopplysningPerson);

        SoeknadDokument soeknadDokument = new SoeknadDokument();

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        soeknadDokument.arbeidUtland = new ArrayList<>();
        soeknadDokument.arbeidUtland.add(arbeidUtland);

        soeknadDokument.oppholdUtland.oppholdsPeriode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));

        Saksopplysning saksopplysningSøknad = new Saksopplysning();
        saksopplysningSøknad.setType(SaksopplysningType.SØKNAD);
        saksopplysningSøknad.setDokument(soeknadDokument);
        saksopplysninger.add(saksopplysningSøknad);

        behandling.setSaksopplysninger(saksopplysninger);

        when(prosessinstansRepository.findByStegIsNotNullAndStegIsNotAndBehandling_Id(eq(ProsessSteg.FEILET_MASKINELT), anyLong())).thenReturn(Optional.empty());
        when(behandlingRepo.findOneWithSaksopplysningerById(anyLong())).thenReturn(behandling);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn("12345");

        saksopplysningerService.oppfriskSaksopplysning(13L);

        assertThat(behandling.getSaksopplysninger().size()).isEqualTo(1);
        assertThat(behandling.getSaksopplysninger().stream().findFirst().get().getType()).isEqualTo(SaksopplysningType.SØKNAD);
        verify(behandlingsresultatService, times(1)).tømBehandlingsresultat(anyLong());
        verify(binge, times(1)).leggTil(any());
    }
}