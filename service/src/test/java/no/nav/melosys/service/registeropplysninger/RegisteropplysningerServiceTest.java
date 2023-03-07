package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKildesystem;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.utbetaling.UtbetaldataRestService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.sob.SobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisteropplysningerServiceTest {

    private static final String AKTØR_ID = "123321";
    private static final String FNR = "432234";

    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private EregFasade eregFasade;
    @Mock
    private AaregFasade aaregFasade;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private SobService sobService;
    @Mock
    private InntektService inntektService;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private RegisteropplysningerPeriodeFactory registeropplysningerPeriodeFactory;
    @Mock
    private UtbetaldataRestService utbetaldataRestService;

    private final FakeUnleash unleash = new FakeUnleash();

    private RegisteropplysningerService registeropplysningerService;

    @BeforeEach
    public void setUp() throws Exception {
        unleash.enableAll();
        registeropplysningerService = new RegisteropplysningerService(persondataFasade, medlPeriodeService, eregFasade, aaregFasade, behandlingService,
            sobService, inntektService, saksopplysningerService, registeropplysningerPeriodeFactory, unleash, utbetaldataRestService);
        when(persondataFasade.hentAktørIdForIdent(anyString())).thenReturn(AKTØR_ID);

        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.ARBFORH));
        when(medlPeriodeService.hentPeriodeListe(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.MEDL));
        when(inntektService.hentInntektListe(anyString(), anyYearMonth(), anyYearMonth())).thenReturn(lagSaksopplysning(SaksopplysningType.INNTK));
        when(eregFasade.hentOrganisasjon(anyString())).thenReturn(lagSaksopplysning(SaksopplysningType.ORG));
        when(sobService.finnSakOgBehandlingskjedeListe(anyString())).thenReturn(lagSaksopplysning(SaksopplysningType.SOB_SAK));

        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(hentBehandling());
        when(saksopplysningerService.finnArbeidsforholdsopplysninger(anyLong())).thenReturn(Optional.of(lagArbeidsforholdDokument()));
        when(saksopplysningerService.finnInntektsopplysninger(anyLong())).thenReturn(Optional.empty());

        when(registeropplysningerPeriodeFactory.hentPeriodeForArbeidsforhold(anyLocalDate(), anyLocalDate())).thenReturn(hentDatoPeriode());
        when(registeropplysningerPeriodeFactory.hentPeriodeForMedlemskap(anyLocalDate(), anyLocalDate(), anyBehandling())).thenReturn(hentDatoPeriode());
        when(registeropplysningerPeriodeFactory.hentPeriodeForInntekt(anyLocalDate(), anyLocalDate(), anyBehandling())).thenReturn(hentPeriode());
    }

    @Test
    void hentOgLagreOpplysninger_medAlleOpplysninger_alleBlirHentetOgLagret() {
        unleash.disableAll();

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(2L)
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder()
                    .arbeidsforholdopplysninger()
                    .inntektsopplysninger()
                    .medlemskapsopplysninger()
                    .organisasjonsopplysninger()
                    .sakOgBehandlingopplysninger()
                    .utbetalingsopplysninger()
                    .build())
                .fom(LocalDate.now().minusYears(1))
                .tom(LocalDate.now().plusYears(1))
                .fnr(FNR)
                .build());

        verify(behandlingService).lagre(any(Behandling.class));

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate());
        verify(inntektService).hentInntektListe(anyString(), anyYearMonth(), anyYearMonth());
        verify(medlPeriodeService).hentPeriodeListe(anyString(), anyLocalDate(), anyLocalDate());
        verify(eregFasade).hentOrganisasjon(anyString());
        verify(sobService).finnSakOgBehandlingskjedeListe(AKTØR_ID);
    }

    @Test
    void hentOgLagreOpplysninger_medAlleOpplysningerIVilkårligRekkefølge_alleBlirHentetOgLagretIRettRekkefølge() {
        unleash.disableAll();
        
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = "123456789";

        ArbeidsforholdDokument arbeidsforholdDokument = new ArbeidsforholdDokument(List.of(arbeidsforhold));
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(arbeidsforholdDokument);
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.AAREG, null);
        when(behandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(hentBehandling(saksopplysning));


        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(2L)
                .saksopplysningTyper(RegisteropplysningerRequest.hentSaksopplysningTyperSomLagres())
                .fom(LocalDate.now().minusYears(1))
                .tom(LocalDate.now().plusYears(1))
                .fnr(FNR)
                .build());


        verify(behandlingService).lagre(any(Behandling.class));

        // Noen av stegene er avhengige av hverandre. Det er viktig at vi ivaretar rekkefølgen.
        InOrder inntektFørOrg = inOrder(inntektService, eregFasade);
        inntektFørOrg.verify(inntektService).hentInntektListe(anyString(), anyYearMonth(), anyYearMonth());
        inntektFørOrg.verify(eregFasade).hentOrganisasjon(anyString());

        InOrder arbeidsforholdFørOrg = inOrder(aaregFasade, eregFasade);
        arbeidsforholdFørOrg.verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate());
        arbeidsforholdFørOrg.verify(eregFasade).hentOrganisasjon(anyString());

        verify(medlPeriodeService).hentPeriodeListe(anyString(), anyLocalDate(), anyLocalDate());
        verify(sobService).finnSakOgBehandlingskjedeListe(AKTØR_ID);
    }

    @Test
    void hentArbeidsforholdopplysninger() {
        LocalDate fom = LocalDate.now().minusMonths(1);
        LocalDate tom = LocalDate.now();
        when(aaregFasade.finnArbeidsforholdPrArbeidstaker(anyString(), anyLocalDate(), anyLocalDate())).thenReturn(lagSaksopplysning(SaksopplysningType.ARBFORH));

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().arbeidsforholdopplysninger().build())
            .build());

        verify(aaregFasade).finnArbeidsforholdPrArbeidstaker(eq(FNR), anyLocalDate(), anyLocalDate());
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    void hentMedlemskapsopplysninger() {
        LocalDate fom = LocalDate.now().minusYears(1);
        LocalDate tom = LocalDate.now().plusYears(1);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(medlPeriodeService.hentPeriodeListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().medlemskapsopplysninger().build())
            .build());

        verify(medlPeriodeService).hentPeriodeListe(anyString(), any(), any());
        verify(behandlingService).lagre(any(Behandling.class));
    }

    @Test
    void hentInntektsopplysninger() {
        LocalDate fom = LocalDate.now().minusYears(3);
        LocalDate tom = LocalDate.now().minusYears(2);
        Saksopplysning saksopplysning = hentSedSaksopplysning(fom, tom);
        when(inntektService.hentInntektListe(anyString(), any(), any())).thenReturn(saksopplysning);

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest(fom, tom)
            .saksopplysningTyper(saksopplysningstyper().inntektsopplysninger().build())
            .build());

        verify(inntektService).hentInntektListe(anyString(), anyYearMonth(), anyYearMonth());
        verify(behandlingService).lagre(any(Behandling.class));
    }


    @Test
    void hentOgLagreOpplysninger_feilIPeriode_kanIkkeHenteOpplysningerSomBrukerPeriode() {
        LocalDate fom = LocalDate.now().plusYears(2);
        LocalDate tom = LocalDate.now();

        registeropplysningerService.hentOgLagreOpplysninger(RegisteropplysningerRequest.builder()
            .behandlingID(2L)
            .saksopplysningTyper(RegisteropplysningerRequest.hentSaksopplysningTyperSomLagres())
            .fnr(FNR)
            .fom(fom)
            .tom(tom)
            .build());

        verify(aaregFasade, never()).finnArbeidsforholdPrArbeidstaker(anyString(), any(), any());
        verify(inntektService, never()).hentInntektListe(anyString(), any(), any());
        verify(medlPeriodeService, never()).hentPeriodeListe(anyString(), any(), any());

        verify(eregFasade).hentOrganisasjon(anyString());
        verify(sobService).finnSakOgBehandlingskjedeListe(AKTØR_ID);
        verify(behandlingService).lagre(any(Behandling.class));
    }

    private Behandling hentBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(2L);
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

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
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.AAREG, null);

        return arbeidsforholdDokument;
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

    private Behandling anyBehandling() {
        return any(Behandling.class);
    }

    private RegisteropplysningerPeriodeFactory.Periode hentPeriode() {
        return new RegisteropplysningerPeriodeFactory.Periode(YearMonth.now(), YearMonth.now());
    }

    private RegisteropplysningerPeriodeFactory.DatoPeriode hentDatoPeriode() {
        return new RegisteropplysningerPeriodeFactory.DatoPeriode(LocalDate.now(), LocalDate.now());
    }
}
