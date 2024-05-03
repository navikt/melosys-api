package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OppfriskSaksopplysningerServiceTest {
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private UfmKontrollService ufmKontrollService;
    @Mock
    private InngangsvilkaarService inngangsvilkaarService;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;

    private OppfriskSaksopplysningerService oppfriskSaksopplysningerService;

    private static final long BEHANDLING_ID = 11L;

    @BeforeEach
    public void setUp() {
        RegisteropplysningerFactory registeropplysningerFactory = new RegisteropplysningerFactory(saksbehandlingRegler);
        oppfriskSaksopplysningerService = new OppfriskSaksopplysningerService(
            anmodningsperiodeService,
            behandlingService,
            behandlingsresultatService,
            ufmKontrollService,
            inngangsvilkaarService,
            registeropplysningerService,
            persondataFasade,
            registeropplysningerFactory);
    }

    @Test
    void oppfriskSaksopplysning() {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(persondataFasade.hentFolkeregisterident(anyString())).thenReturn("322211");


        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);


        verify(behandlingsresultatService).tømBehandlingsresultat(anyLong());
        verify(registeropplysningerService).slettRegisterOpplysninger(BEHANDLING_ID);
        verify(registeropplysningerService).hentOgLagreOpplysninger(any(RegisteropplysningerRequest.class));
    }

    @Test
    void oppfriskSaksopplysning_virksomhetIngenFlyt() {
        Behandling behandling = lagBehandling();
        Aktoer virksomhet = new Aktoer();
        virksomhet.setRolle(Aktoersroller.VIRKSOMHET);
        behandling.getFagsak().leggTilAktør(virksomhet);
        behandling.setType(Behandlingstyper.HENVENDELSE);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any())).thenReturn(true);
        when(inngangsvilkaarService.skalVurdereInngangsvilkår(any())).thenReturn(false);


        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);


        verify(behandlingsresultatService).tømBehandlingsresultat(anyLong());
        verify(registeropplysningerService).slettRegisterOpplysninger(BEHANDLING_ID);
        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any(Periode.class));
    }

    @Test
    void oppfriskSaksopplysning_anmodningOmUnntakSendt_feiler() {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        lagBehandling().setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        when(anmodningsperiodeService.harSendtAnmodningsperiode(BEHANDLING_ID)).thenReturn(true);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false))
            .withMessageContaining("Anmodning om unntak er sendt");
    }

    @Test
    void oppfriskSaksopplysning_medSED_kallerKontroller() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE);

        behandling.getSaksopplysninger().add(lagSED());
        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(persondataFasade.hentFolkeregisterident(anyString())).thenReturn("322211");


        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);


        verify(ufmKontrollService).utførKontrollerOgRegistrerFeil(BEHANDLING_ID);
    }

    @Test
    void oppfriskSaksopplysning_harIkkeOppfyltInngangsvilkår_oppdatererType() {
        Behandling behandling = lagBehandling();
        behandling.getFagsak().setType(Sakstyper.EU_EOS);

        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setVilkaar(Vilkaar.FO_883_2004_INNGANGSVILKAAR);
        vilkaarsresultat.setOppfylt(false);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.getVilkaarsresultater().add(vilkaarsresultat);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(persondataFasade.hentFolkeregisterident(anyString())).thenReturn("322211");
        when(inngangsvilkaarService.skalVurdereInngangsvilkår(any())).thenReturn(true);
        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(anyLong(), anyList(), anyBoolean(), any(Periode.class))).thenReturn(true);


        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);


        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(eq(behandling.getId()), eq(List.of("SE")), eq(false), any(Periode.class));
    }

    @Test
    void oppfriskSaksopplysning_skalIkkeHenteInngangsvilkår_henterIkkeInngangsvilkår() {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(persondataFasade.hentFolkeregisterident(anyString())).thenReturn("322211");
        when(inngangsvilkaarService.skalVurdereInngangsvilkår(any())).thenReturn(false);


        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);


        verify(inngangsvilkaarService, never()).vurderOgLagreInngangsvilkår(anyLong(), any(), anyBoolean(), any(Periode.class));
    }

    private Saksopplysning lagSED() {
        Saksopplysning sed = new Saksopplysning();
        SedDokument sedDokument = new SedDokument();
        sed.setType(SaksopplysningType.SEDOPPL);
        sed.setDokument(sedDokument);
        var periode = new no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.MIN, LocalDate.MAX);
        sedDokument.setLovvalgsperiode(periode);
        return sed;
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        Fagsak fagsak = FagsakTestFactory.builder().medBruker().build();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);

        HashSet<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSOPL);
        saksopplysninger.add(saksopplysningPerson);

        Soeknad soeknad = new Soeknad();

        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        soeknad.arbeidPaaLand.setFysiskeArbeidssteder(new ArrayList<>());
        soeknad.arbeidPaaLand.getFysiskeArbeidssteder().add(fysiskArbeidssted);

        soeknad.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
        soeknad.soeknadsland = new Soeknadsland(List.of("SE"), false);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(soeknad);
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        behandling.setSaksopplysninger(saksopplysninger);
        return behandling;
    }
}
