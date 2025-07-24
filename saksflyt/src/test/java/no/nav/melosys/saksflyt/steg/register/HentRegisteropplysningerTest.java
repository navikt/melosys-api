package no.nav.melosys.saksflyt.steg.register;

import java.time.LocalDate;

import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HentRegisteropplysningerTest {

    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;

    private HentRegisteropplysninger hentRegisteropplysninger;

    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> requestCaptor;

    private final Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();

    private FakeUnleash fakeUnleash = new FakeUnleash();

    @BeforeEach
    public void setUp() {
        RegisteropplysningerFactory registeropplysningerFactory = new RegisteropplysningerFactory(saksbehandlingRegler, fakeUnleash);
        hentRegisteropplysninger = new HentRegisteropplysninger(registeropplysningerService, behandlingService, saksbehandlingRegler, persondataFasade, registeropplysningerFactory);

        behandling.setId(222L);

        Fagsak fagsak = FagsakTestFactory.builder().medBruker().build();
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.FØRSTEGANG);

        when(behandlingService.hentBehandling(behandling.getId())).thenReturn(behandling);
    }

    @Test
    void utfør_hoppOverSteg() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        Fagsak fagsak = FagsakTestFactory.builder().type(Sakstyper.FTRL).medBruker().build();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.ARBEID_KUN_NORGE);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_hoppOverSteg_virksomhet() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        Fagsak fagsak = FagsakTestFactory.builder().type(Sakstyper.FTRL).medVirksomhet().build();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.ARBEID_KUN_NORGE);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_behandlingstemaUtsendtArbeidstaker_henterPeriodeFraSøknad() {
        String ident = "143545";
        when(persondataFasade.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID)).thenReturn(ident);

        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Periode periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(new Soeknad());
        mottatteOpplysninger.getMottatteOpplysningerData().periode = periode;
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);


        hentRegisteropplysninger.utfør(prosessinstans);


        verify(registeropplysningerService).hentOgLagreOpplysninger(requestCaptor.capture());

        assertThat(requestCaptor.getValue())
            .extracting(RegisteropplysningerRequest::getBehandlingID, RegisteropplysningerRequest::getFnr, RegisteropplysningerRequest::getFom, RegisteropplysningerRequest::getTom)
            .containsExactly(behandling.getId(), ident, periode.getFom(), periode.getTom());
    }

    @Test
    void utfør_sakstypeFtrl_ingentingLagres() {
        behandling.setTema(Behandlingstema.YRKESAKTIV);
        behandling.getFagsak().setType(Sakstyper.FTRL);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(new SøknadNorgeEllerUtenforEØS());
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_sakstypeTrygdeavtale_ingentingLagres() {
        behandling.setTema(Behandlingstema.YRKESAKTIV);
        behandling.getFagsak().setType(Sakstyper.TRYGDEAVTALE);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(new SøknadNorgeEllerUtenforEØS());
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_sakstypeEøsOgUnntak_ingentingLagres() {
        behandling.setTema(Behandlingstema.A1_ANMODNING_OM_UNNTAK_PAPIR);
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        when(saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)).thenReturn(true);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(new SøknadNorgeEllerUtenforEØS());
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_sakstypeEøsOgIkkeYrkesaktiv_ingentingLagres() {
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        when(saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)).thenReturn(true);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(new SøknadNorgeEllerUtenforEØS());
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_harIngenFlyt_henterIngenting() {
        behandling.setTema(Behandlingstema.TRYGDETID);
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        when(saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any())).thenReturn(true);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService).hentOgLagreOpplysninger(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getOpplysningstyper()).isEmpty();
    }
}
