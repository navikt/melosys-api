package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.utbetaldata.UtbetaldataService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisteropplysningerServiceTest {

    private static final String AKTØR_ID = "123321";
    private static final String FNR = "432234";

    @Mock
    private TpsFasade tpsFasade;
    @Mock
    private MedlFasade medlFasade;
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

    private Integer arbeidsforholdhistorikkAntallMåneder = 6;
    private Integer medlemskaphistorikkAntallÅr = 5;

    private RegisteropplysningerService registeropplysningerService;

    @Before
    public void setUp() throws Exception {
        registeropplysningerService = new RegisteropplysningerService(tpsFasade, medlFasade, eregFasade, aaregFasade, behandlingService,
            sakOgBehandlingFasade, inntektService, utbetaldataService, saksopplysningerService, arbeidsforholdhistorikkAntallMåneder, medlemskaphistorikkAntallÅr);
        when(tpsFasade.hentAktørIdForIdent(anyString())).thenReturn(AKTØR_ID);

        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.ARBFORH));
        when(tpsFasade.hentPersonMedAdresse(anyString())).thenReturn(lagSaksopplysning(SaksopplysningType.PERSOPL));
        when(medlFasade.hentPeriodeListe(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.MEDL));
        when(inntektService.hentInntektListe(anyString(), anyYearMonth(), anyYearMonth())).thenReturn(lagSaksopplysning(SaksopplysningType.INNTK));
        when(utbetaldataService.hentUtbetalingerBarnetrygd(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.UTBETAL));
        when(eregFasade.hentOrganisasjon(anyString())).thenReturn(lagSaksopplysning(SaksopplysningType.ORG));
        when(tpsFasade.hentPersonhistorikk(anyString(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.PERSHIST));
        when(sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(anyString())).thenReturn(lagSaksopplysning(SaksopplysningType.SOB_SAK));

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(hentBehandling());
        when(saksopplysningerService.finnArbeidsforholdsopplysninger(anyLong())).thenReturn(Optional.of(lagArbeidsforholdDokument()));
        when(saksopplysningerService.finnInntektsopplysninger(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    public void hentOgLagreOpplysninger_medAlleOpplysninger_alleBlirHentetOgLagret() throws MelosysException {
        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(2L)
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .arbeidsforholdopplysninger()
                    .inntektsopplysninger()
                    .medlemskapsopplysninger()
                    .organisasjonsopplysninger()
                    .personhistorikkopplysninger()
                    .personopplysninger()
                    .sakOgBehandlingopplysninger()
                    .utbetalingsopplysninger()
                    .build())
                .fom(LocalDate.now().minusYears(1))
                .tom(LocalDate.now().plusYears(1))
                .fnr(FNR)
                .build());

        verify(behandlingService).lagre(any(Behandling.class));
        verify(behandlingService, never()).hentBehandling(anyLong());

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate());
        verify(inntektService).hentInntektListe(anyString(), anyYearMonth(), anyYearMonth());
        verify(medlFasade).hentPeriodeListe(anyString(), anyLocalDate(), anyLocalDate());
        verify(eregFasade).hentOrganisasjon(anyString());
        verify(tpsFasade).hentPersonhistorikk(anyString(), anyLocalDate());
        verify(tpsFasade).hentPersonMedAdresse(anyString());
        verify(sakOgBehandlingFasade).finnSakOgBehandlingskjedeListe(eq(AKTØR_ID));
        verify(utbetaldataService).hentUtbetalingerBarnetrygd(anyString(), anyLocalDate(), anyLocalDate());
    }

    @Test
    public void hentOgLagreOpplysninger_medAlleOpplysningerIVilkårligRekkefølge_alleBlirHentetOgLagretIRettRekkefølge() throws MelosysException {
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = "123456789";

        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument(List.of(arbeidsforhold));
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(arbeidsforholdDokument);
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.setKilde(SaksopplysningKilde.AAREG);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(hentBehandling(saksopplysning));

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(2L)
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .sakOgBehandlingopplysninger()
                    .medlemskapsopplysninger()
                    .personhistorikkopplysninger()
                    .organisasjonsopplysninger()
                    .utbetalingsopplysninger()
                    .arbeidsforholdopplysninger()
                    .personopplysninger()
                    .inntektsopplysninger()
                    .build())
                .fom(LocalDate.now().minusYears(1))
                .tom(LocalDate.now().plusYears(1))
                .fnr(FNR)
                .build());

        verify(behandlingService).lagre(any(Behandling.class));
        verify(behandlingService, never()).hentBehandling(anyLong());

        // Noen av stegene er avhengige av hverandre. Det er viktig at vi ivaretar rekkefølgen.
        InOrder inntektFørOrg = inOrder(inntektService, eregFasade);
        inntektFørOrg.verify(inntektService).hentInntektListe(anyString(), anyYearMonth(), anyYearMonth());
        inntektFørOrg.verify(eregFasade).hentOrganisasjon(anyString());

        InOrder arbeidsforholdFørOrg = inOrder(aaregFasade, eregFasade);
        arbeidsforholdFørOrg.verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate());
        arbeidsforholdFørOrg.verify(eregFasade).hentOrganisasjon(anyString());

        verify(medlFasade).hentPeriodeListe(anyString(), anyLocalDate(), anyLocalDate());
        verify(tpsFasade).hentPersonhistorikk(anyString(), anyLocalDate());
        verify(tpsFasade).hentPersonMedAdresse(anyString());
        verify(sakOgBehandlingFasade).finnSakOgBehandlingskjedeListe(eq(AKTØR_ID));
        verify(utbetaldataService).hentUtbetalingerBarnetrygd(anyString(), anyLocalDate(), anyLocalDate());
    }

    @Test
    public void hentArbeidsforholdopplysninger() throws MelosysException {
        LocalDate fom = LocalDate.now().minusMonths(1);
        LocalDate tom = LocalDate.now();
        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.ARBFORH));

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().arbeidsforholdopplysninger().build())
            .build());

        LocalDate forventetFom = fom.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        LocalDate forventetTom = LocalDate.now();

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(eq(FNR), eq(forventetFom), eq(forventetTom));
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void utfoerSteg_fremtidigPeriode() throws MelosysException {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);
        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.ARBFORH));

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().arbeidsforholdopplysninger().build())
            .build());

        LocalDate forventetFom = LocalDate.now().minusMonths(arbeidsforholdhistorikkAntallMåneder);
        LocalDate forventetTom = LocalDate.now();

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(eq(FNR), eq(forventetFom), eq(forventetTom));
        verify(behandlingService).lagre(any(Behandling.class));
    }


    @Test
    public void hentPersonopplysninger() throws MelosysException {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("123");
        behandling.setFagsak(fagsak);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest()
            .saksopplysningTyper(saksopplysningstyper().personopplysninger().build())
            .build());

        verify(tpsFasade).hentPersonMedAdresse(FNR);
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentMedlemskapsopplysninger() throws MelosysException {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(medlFasade.hentPeriodeListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().medlemskapsopplysninger().build())
            .build());

        verify(medlFasade).hentPeriodeListe(anyString(), any(), any());
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentInntektsopplysninger_tomTilDato_forespørTomTilDato() throws MelosysException {
        LocalDate fom = LocalDate.now().minusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, null);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, null)
            .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
            .build());

        verify(inntektService).hentInntektListe(anyString(), eq(YearMonth.from(fom)), eq(YearMonth.from(fom.plusYears(2))));
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentInntektsopplysninger_periodePåbegynt_verifiserInntektPeriode() throws MelosysException {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
            .build());

        verify(inntektService).hentInntektListe(anyString(), eq(YearMonth.from(fom.minusMonths(2))), eq(YearMonth.from(tom)));
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentInntektsopplysninger_periodeIkkePåbegynt_verifiserInntektPeriode() throws MelosysException {
        LocalDate fom = LocalDate.now().plusYears(1);
        LocalDate tom = LocalDate.now().plusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
            .build());

        verify(inntektService).hentInntektListe(anyString(), eq(YearMonth.from(LocalDate.now().minusMonths(2))), eq(YearMonth.from(LocalDate.now())));
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentInntektsopplysninger_periodeAvsluttet_verifiserInntektPeriode() throws MelosysException {
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(new Saksopplysning());
        LocalDate fom = LocalDate.now().minusYears(3);
        LocalDate tom = LocalDate.now().minusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
            .build());

        verify(inntektService).hentInntektListe(anyString(), eq(YearMonth.from(fom)), eq(YearMonth.from(tom)));
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentUtbetalingsopplysninger() throws MelosysException {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(utbetaldataService.hentUtbetalingerBarnetrygd(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().utbetalingsopplysninger().build())
            .build());

        verify(utbetaldataService).hentUtbetalingerBarnetrygd(anyString(), any(), any());
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentUtbetalingsopplysninger_periode5ÅrTilbakeITid_kanIkkeHenteUtbetalOpplysninger() throws MelosysException {
        LocalDate fom = LocalDate.now().minusYears(medlemskaphistorikkAntallÅr);
        LocalDate tom = LocalDate.now().minusYears(4);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().utbetalingsopplysninger().build())
            .build());

        verify(utbetaldataService, never()).hentUtbetalingerBarnetrygd(anyString(), any(), any());
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    public void hentOgLagreOpplysninger_kunBehandlingID_forventHentBehandling() throws MelosysException {
        registeropplysningerService.hentOgLagreOpplysninger(RegisteropplysningerRequest.builder()
            .fnr(FNR)
            .behandlingID(1L)
            .saksopplysningTyper(saksopplysningstyper().personopplysninger().build())
            .build());

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(eq(1L));
    }

    private Behandling hentBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(2L);
        behandling.setType(Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        return behandling;
    }

    private Behandling hentBehandling(Saksopplysning saksopplysning) {
        Behandling behandling = hentBehandling();
        behandling.getSaksopplysninger().add(saksopplysning);

        return behandling;
    }

    private Saksopplysning hentSedSaksopplysning(LocalDate fom, LocalDate tom) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(hentSedDokument(fom, tom));
        saksopplysning.setType(SaksopplysningType.SEDOPPL);
        return saksopplysning;
    }

    private SedDokument hentSedDokument(LocalDate fom, LocalDate tom) {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgsperiode(new no.nav.melosys.domain.dokument.medlemskap.Periode(fom, tom));
        sedDokument.setFnr("123");
        return sedDokument;
    }

    private Saksopplysning lagSaksopplysning(SaksopplysningType saksopplysningType) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(saksopplysningType);

        return saksopplysning;
    }

    private ArbeidsforholdDokument lagArbeidsforholdDokument() {
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = "123456789";

        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument(List.of(arbeidsforhold));
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(arbeidsforholdDokument);
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.setKilde(SaksopplysningKilde.AAREG);

        return arbeidsforholdDokument;
    }

    private RegisteropplysningerRequest.RegisteropplysningerRequestBuilder registeropplysningerRequest() {
        return registeropplysningerRequest(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
    }

    private RegisteropplysningerRequest.RegisteropplysningerRequestBuilder registeropplysningerRequest(LocalDate fom, LocalDate tom) {
        return RegisteropplysningerRequest.builder()
            .behandlingID(2L)
            .fom(fom)
            .tom(tom)
            .fnr(FNR);
    }

    private RegisteropplysningerRequest.SaksopplysningTyper.SaksopplysningTyperBuilder saksopplysningstyper() {
        return RegisteropplysningerRequest.SaksopplysningTyper.builder();
    }

    private LocalDate anyLocalDate() {
        return any(LocalDate.class);
    }

    private YearMonth anyYearMonth() {
        return any(YearMonth.class);
    }

    private Saksopplysning anySaksopplysning() {
        return any(Saksopplysning.class);
    }
}