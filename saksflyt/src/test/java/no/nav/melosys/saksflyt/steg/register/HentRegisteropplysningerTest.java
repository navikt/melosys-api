package no.nav.melosys.saksflyt.steg.register;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.SoeknadFtrl;
import no.nav.melosys.domain.mottatteopplysninger.SoeknadTrygdeavtale;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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

    private RegisteropplysningerFactory registeropplysningerFactory;

    private HentRegisteropplysninger hentRegisteropplysninger;

    @Captor
    private ArgumentCaptor<RegisteropplysningerRequest> requestCaptor;

    private final Behandling behandling = new Behandling();
    private final String aktørID = "54321";

    @BeforeEach
    public void setUp() {
        registeropplysningerFactory = new RegisteropplysningerFactory(saksbehandlingRegler, new FakeUnleash());
        hentRegisteropplysninger = new HentRegisteropplysninger(registeropplysningerService, behandlingService, persondataFasade, registeropplysningerFactory);

        behandling.setId(222L);

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId(aktørID);

        Fagsak fagsak = new Fagsak();
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        fagsak.getAktører().add(bruker);
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.FØRSTEGANG);

        when(behandlingService.hentBehandling(behandling.getId())).thenReturn(behandling);
    }

    @Test
    void utfør_hoppOverSteg() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.FTRL);
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Collections.singleton(bruker));
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.ARBEID_KUN_NORGE);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_hoppOverSteg_virksomhet() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        Fagsak fagsak = new Fagsak();

        fagsak.setAktører(new HashSet<>());
        Aktoer a1 = new Aktoer();
        a1.setRolle(Aktoersroller.VIRKSOMHET);
        a1.setAktørId("123");
        fagsak.getAktører().add(a1);

        fagsak.setType(Sakstyper.FTRL);
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.ARBEID_KUN_NORGE);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_behandlingstemaUtsendtArbeidstaker_henterPeriodeFraSøknad() {
        String ident = "143545";
        when(persondataFasade.hentFolkeregisterident(aktørID)).thenReturn(ident);

        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        Periode periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(new Soeknad());
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
        behandling.setTema(Behandlingstema.ARBEID_I_UTLANDET);
        behandling.getFagsak().setType(Sakstyper.FTRL);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(new SoeknadFtrl());
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
        mottatteOpplysninger.setMottatteOpplysningerdata(new SoeknadTrygdeavtale());
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utfør_harTomFlyt_henterIngenting() {
        behandling.setTema(Behandlingstema.TRYGDETID);
        behandling.getFagsak().setType(Sakstyper.EU_EOS);
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        when(saksbehandlingRegler.harTomFlyt(any())).thenReturn(true);

        hentRegisteropplysninger.utfør(prosessinstans);

        verify(registeropplysningerService).hentOgLagreOpplysninger(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getOpplysningstyper()).isEmpty();
    }
}
