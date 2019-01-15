package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoerroller;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class SaksopplysningerServiceTest {

    private SaksopplysningerService saksopplysningerService;

    @Mock
    private TpsFasade tpsFasade;

    @Mock
    private AaregFasade aaregFasade;

    @Mock
    private ProsessinstansRepository prosessinstansRepository;

    @Mock
    private BehandlingRepository behandlingRepo;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    @Mock
    private Binge binge;

    @Before
    public void setUp() {
        saksopplysningerService = new SaksopplysningerService(tpsFasade, aaregFasade, prosessinstansRepository, binge, behandlingRepo, behandlingsresultatService);

        ReflectionTestUtils.setField(saksopplysningerService, "arbeidsforholdhistorikkAntallMåneder", 6);
        ReflectionTestUtils.setField(saksopplysningerService, "inntektshistorikkAntallMåneder", 6);
        ReflectionTestUtils.setField(saksopplysningerService, "medlemskaphistorikkAntallÅr", 5);
    }

    @Test
    public void hentArbeidsforholdHistorikk() throws MelosysException {
        final Long arbeidsforholdsID = 12608035L;
        when(aaregFasade.hentArbeidsforholdHistorikk(eq(arbeidsforholdsID))).thenReturn(new Saksopplysning());
        saksopplysningerService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
        verify(aaregFasade).hentArbeidsforholdHistorikk(eq(arbeidsforholdsID));
    }

    @Test
    public void oppfriskSaksopplysning() throws IkkeFunnetException, TekniskException {

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        Aktoer aktør = new Aktoer();
        aktør.setAktørId("123");
        aktør.setRolle(Aktoerroller.BRUKER);
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
        when(behandlingRepo.findWithSaksopplysningerById(anyLong())).thenReturn(behandling);
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn("12345");

        saksopplysningerService.oppfriskSaksopplysning(13L);

        assertThat(behandling.getSaksopplysninger().size()).isEqualTo(1);
        assertThat(behandling.getSaksopplysninger().stream().findFirst().get().getType()).isEqualTo(SaksopplysningType.SØKNAD);
        verify(behandlingsresultatService, times(1)).tømBehandlingsresultat(anyLong());
        verify(binge, times(1)).leggTil(any());
    }

    @Test
    public void sjekkStatusBehandlingForOppfrisking() {
        when(prosessinstansRepository.findByTypeAndStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessType.OPPFRISKNING, ProsessSteg.FEILET_MASKINELT, 111L)).thenReturn(Optional.empty());
        assertThat(saksopplysningerService.harAktivOppfrisking(111L)).isFalse();

        Prosessinstans process = mock(Prosessinstans.class);
        when(prosessinstansRepository.findByTypeAndStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessType.OPPFRISKNING, ProsessSteg.FEILET_MASKINELT, 111L)).thenReturn(Optional.of(process));
        assertThat(saksopplysningerService.harAktivOppfrisking(111L)).isTrue();
    }

}