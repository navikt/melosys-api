package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentRegisteropplysningerTest {

    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private AaregFasade aaregFasade;
    @Mock
    private InntektService inntektService;
    @Mock
    private EregFasade eregFasade;
    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private BehandlingService behandlingService;
    @InjectMocks
    private RegisteropplysningerService registeropplysningerService;

    private HentRegisteropplysninger agent;
    private Behandling behandling;

    private final int ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER = 6;
    private final int INNTEKTSHISTORIKK_ANTALL_MÅNEDER = 6;
    private final int MEDLEMSKAPHISTORIKK_ANTALL_ÅR = 5;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        agent = new HentRegisteropplysninger(registeropplysningerService);

        ReflectionTestUtils.setField(registeropplysningerService, "arbeidsforholdhistorikkAntallMåneder", ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
        ReflectionTestUtils.setField(registeropplysningerService, "inntektshistorikkAntallMåneder", INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
        ReflectionTestUtils.setField(registeropplysningerService, "medlemskaphistorikkAntallÅr", MEDLEMSKAPHISTORIKK_ANTALL_ÅR);

        behandling = new Behandling();
        behandling.setId(222L);
        behandling.setFagsak(new Fagsak());
        behandling.setSaksopplysninger(new HashSet<>());
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument();
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = "orgNr";
        arbeidsforholdDokument.getArbeidsforhold().add(arbeidsforhold);
        when(saksopplysningerService.finnArbeidsforholdsopplysninger(anyLong())).thenReturn(Optional.of(arbeidsforholdDokument));

        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn("aktørId");

        when(tpsFasade.hentPersonMedAdresse(any())).thenReturn(lagSaksopplysning(SaksopplysningType.PERSOPL));
        when(tpsFasade.hentPersonhistorikk(any(), any())).thenReturn(lagSaksopplysning(SaksopplysningType.PERSHIST));
        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(any(), any(), any())).thenReturn(lagSaksopplysning(SaksopplysningType.ARBFORH));
        when(inntektService.hentInntektListe(any(), any(), any())).thenReturn(lagSaksopplysning(SaksopplysningType.INNTK));
        when(eregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning(SaksopplysningType.ORG));
        when(medlPeriodeService.hentPeriodeListe(any(), any(), any())).thenReturn(lagSaksopplysning(SaksopplysningType.MEDL));
        when(sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(any())).thenReturn(lagSaksopplysning(SaksopplysningType.SOB_SAK));
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().minusMonths(1), LocalDate.now());

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, new Periode(periode.getFom(), periode.getTom()));

        agent.utførSteg(p);

        // Personopplysninger
        verify(tpsFasade).hentPersonMedAdresse(brukerID);
        verify(tpsFasade).hentPersonhistorikk(brukerID, periode.getFom());

        // Arbeidsforhold
        LocalDate forventetArbeidsforholdFom = periode.getFom().minusMonths(ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
        LocalDate forventetArbeidsforholdTom = periode.getTom();
        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(brukerID, forventetArbeidsforholdFom, forventetArbeidsforholdTom);

        // Inntekt
        YearMonth forventetInntektFom = YearMonth.from(periode.getFom()).minusMonths(INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
        YearMonth forventetInntektTom = YearMonth.now();
        verify(inntektService).hentInntektListe(brukerID, forventetInntektFom, forventetInntektTom);

        // Organisasjon
        verify(eregFasade).hentOrganisasjon("orgNr");

        // Medlemskap
        LocalDate forventetMedlemskapFom = periode.getFom().minusYears(MEDLEMSKAPHISTORIKK_ANTALL_ÅR);
        LocalDate forventetMedlemskapTom = periode.getTom();
        verify(medlPeriodeService).hentPeriodeListe(brukerID, forventetMedlemskapFom, forventetMedlemskapTom);

        // Sak og behandling
        verify(sakOgBehandlingFasade).finnSakOgBehandlingskjedeListe("aktørId");

        // Saksopplysninger
        ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
        verify(behandlingService).lagre(behandlingCaptor.capture());
        assertThat(behandlingCaptor.getValue().getSaksopplysninger().size()).isEqualTo(7);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_INNGANGSVILKÅR);
    }

    @Test
    public void utfoerSteg_fremtidigPeriode() throws TekniskException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().plusYears(1), LocalDate.now().plusYears(2));

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        agent.utførSteg(p);

        LocalDate forventetArbeidsforholdFom = LocalDate.now().minusMonths(ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
        LocalDate forventetArbeidsforholdTom = LocalDate.now();
        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(brukerID, forventetArbeidsforholdFom, forventetArbeidsforholdTom);

        YearMonth forventetInntektFom = YearMonth.now().minusMonths(INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
        YearMonth forventetInntektTom = YearMonth.now();
        verify(inntektService).hentInntektListe(brukerID, forventetInntektFom, forventetInntektTom);

        LocalDate forventetMedlemskapFom = periode.getFom().minusYears(MEDLEMSKAPHISTORIKK_ANTALL_ÅR);
        LocalDate forventetMedlemskapTom = periode.getTom();
        verify(medlPeriodeService).hentPeriodeListe(brukerID, forventetMedlemskapFom, forventetMedlemskapTom);

        AssertionsForClassTypes.assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_INNGANGSVILKÅR);
    }

    @Test
    public void utfoerSteg_åpenPeriode() throws TekniskException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);

        String brukerID = "99999999991";
        Periode periode = new Periode(LocalDate.now().minusYears(2), null);

        p.setData(ProsessDataKey.BRUKER_ID, brukerID);
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        agent.utførSteg(p);

        LocalDate forventetArbeidsforholdFom = periode.getFom().minusMonths(ARBEIDSFORHOLD_HISTORIKK_ANTALL_MÅNEDER);
        LocalDate forventetArbeidsforholdTom = periode.getFom().plusYears(1);
        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(brukerID, forventetArbeidsforholdFom, forventetArbeidsforholdTom);

        YearMonth forventetInntektFom = YearMonth.from(periode.getFom()).minusMonths(INNTEKTSHISTORIKK_ANTALL_MÅNEDER);
        YearMonth forventetInntektTom = YearMonth.from(periode.getFom()).plusYears(1);
        verify(inntektService).hentInntektListe(brukerID, forventetInntektFom, forventetInntektTom);

        LocalDate forventetMedlemskapFom = periode.getFom().minusYears(MEDLEMSKAPHISTORIKK_ANTALL_ÅR);
        LocalDate forventetMedlemskapTom = periode.getTom();
        verify(medlPeriodeService).hentPeriodeListe(brukerID, forventetMedlemskapFom, forventetMedlemskapTom);

        AssertionsForClassTypes.assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_VURDER_INNGANGSVILKÅR);
    }

    private Saksopplysning lagSaksopplysning(SaksopplysningType type) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(type);
        return saksopplysning;
    }
}