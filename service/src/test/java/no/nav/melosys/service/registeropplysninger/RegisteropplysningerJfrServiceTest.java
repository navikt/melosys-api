package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.utbetaldata.UtbetaldataService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegisteropplysningerJfrServiceTest extends RegisteropplysningerServiceTestParent {

    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private EregFasade eregFasade;
    @Mock
    private AaregFasade aaregFasade;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private SakOgBehandlingFasade sakOgBehandlingFasade;
    @Mock
    private InntektService inntektService;
    @Mock
    private UtbetaldataService utbetaldataService;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private RegisteropplysningerService registeropplysningerService;

    @Before
    public void setUp() throws Exception {
        registeropplysningerService = new RegisteropplysningerJfrService(tpsFasade, medlPeriodeService, eregFasade, aaregFasade, behandlingService,
            sakOgBehandlingFasade, inntektService, utbetaldataService, saksopplysningerService,
            arbeidsforholdhistorikkAntallMåneder, medlemskaphistorikkAntallÅr, inntektshistorikkAntallMåneder);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(hentBehandling());
    }

    @Test
    public void hentArbeidsforholdopplysninger_åpenPeriode() throws MelosysException {
        LocalDate fom = LocalDate.now().minusYears(2);
        LocalDate tom = null;
        Saksopplysning saksopplysning = lagSaksopplysning(SaksopplysningType.ARBFORH);
        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().arbeidsforholdopplysninger().build())
            .build());

        LocalDate forventetFom = fom.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        LocalDate forventetTom = fom.plusYears(1);

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(eq(FNR), eq(forventetFom), eq(forventetTom));
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentInntektsopplysninger_åpenPeriodeBehandlingSøknad_forespørTomTilDato() throws MelosysException {
        LocalDate fom = LocalDate.now().minusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, null);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, null)
            .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
            .build());

        YearMonth forventetFom = YearMonth.from(fom.minusMonths(inntektshistorikkAntallMåneder));
        YearMonth forventetTom = YearMonth.from(fom.plusYears(1));

        verify(inntektService).hentInntektListe(anyString(), eq(forventetFom), eq(forventetTom));
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentInntektsopplysninger_periodeIkkePåbegyntBehandlingSøknad_verifiserInntektPeriode() throws MelosysException {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
            .build());

        YearMonth forventetInntektFom = YearMonth.now().minusMonths(inntektshistorikkAntallMåneder);
        YearMonth forventetInntektTom = YearMonth.now();

        verify(inntektService).hentInntektListe(anyString(), eq(forventetInntektFom), eq(forventetInntektTom));
        verify(behandlingService).lagre(any(Behandling.class));
    }
}
