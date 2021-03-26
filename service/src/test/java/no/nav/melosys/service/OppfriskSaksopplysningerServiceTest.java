package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.KontrollresultatService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OppfriskSaksopplysningerServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private KontrollresultatService kontrollresultatService;
    @Mock
    private InngangsvilkaarService inngangsvilkaarService;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private PersondataFasade persondataFasade;

    private OppfriskSaksopplysningerService oppfriskSaksopplysningerService;

    private static final long BEHANDLING_ID = 11L;

    @BeforeEach
    public void setUp() throws IkkeFunnetException {
        oppfriskSaksopplysningerService = new OppfriskSaksopplysningerService(
            behandlingService, behandlingsresultatService,
            fagsakService, kontrollresultatService,
            inngangsvilkaarService, registeropplysningerService,
            persondataFasade);

        String brukerID = "322211";
        when(persondataFasade.hentIdentForAktørId(anyString())).thenReturn(brukerID);
    }

    @Test
    void oppfriskSaksopplysning() throws MelosysException {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);

        verify(behandlingsresultatService).tømBehandlingsresultat(anyLong());
        verify(registeropplysningerService).hentOgLagreOpplysninger(any(RegisteropplysningerRequest.class));
    }

    @Test
    void oppfriskSaksopplysning_medSED_kallerKontroller() throws MelosysException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE);

        behandling.getSaksopplysninger().add(lagSED());
        when(behandlingService.hentBehandling(eq(BEHANDLING_ID))).thenReturn(behandling);

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);

        verify(kontrollresultatService).utførKontrollerOgRegistrerFeil(eq(BEHANDLING_ID));
    }

    @Test
    void oppfriskSaksopplysning_sakstypeUkjentErSøknad_oppdatererType() throws MelosysException {
        Behandling behandling = lagBehandling();
        behandling.getFagsak().setType(Sakstyper.UKJENT);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(anyLong(), anyList(), any(Periode.class))).thenReturn(true);

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);

        verify(fagsakService).oppdaterType(eq(behandling.getFagsak()), eq(true));
        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(eq(behandling.getId()), eq(List.of("SE")), any(Periode.class));
    }

    @Test
    void oppfriskSaksopplysning_sakstypeUkjentNorgeUtpekt_oppdatererType() throws MelosysException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandling.getFagsak().setType(Sakstyper.UKJENT);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(anyLong(), anyList(), any(Periode.class))).thenReturn(true);

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);

        verify(fagsakService).oppdaterType(eq(behandling.getFagsak()), eq(true));
        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(eq(behandling.getId()), eq(List.of("NO")), any(Periode.class));
    }

    @Test
    void oppfriskSaksopplysning_utenFamilierelasjoner_girForventetInformasjonsbehov() throws MelosysException {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        ArgumentCaptor<RegisteropplysningerRequest> requestCaptor = ArgumentCaptor.forClass(RegisteropplysningerRequest.class);
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, false);
        verify(registeropplysningerService).hentOgLagreOpplysninger(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getInformasjonsbehov()).isEqualTo(Informasjonsbehov.STANDARD);
    }

    @Test
    void oppfriskSaksopplysning_medFamilierelasjoner_girForventetInformasjonsbehov() throws MelosysException {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        ArgumentCaptor<RegisteropplysningerRequest> requestCaptor = ArgumentCaptor.forClass(RegisteropplysningerRequest.class);
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID, true);
        verify(registeropplysningerService).hentOgLagreOpplysninger(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getInformasjonsbehov()).isEqualTo(Informasjonsbehov.MED_FAMILIERELASJONER);
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
        final String aktørID = "123";
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        Aktoer aktør = new Aktoer();
        aktør.setAktørId(aktørID);
        aktør.setRolle(Aktoersroller.BRUKER);
        HashSet<Aktoer> aktører = new HashSet<>();
        aktører.add(aktør);
        fagsak.setAktører(aktører);
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        HashSet<Saksopplysning> saksopplysninger = new HashSet<>();

        Saksopplysning saksopplysningPerson = new Saksopplysning();
        saksopplysningPerson.setType(SaksopplysningType.PERSOPL);
        saksopplysninger.add(saksopplysningPerson);

        Soeknad soeknad = new Soeknad();

        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        soeknad.arbeidPaaLand.fysiskeArbeidssteder = new ArrayList<>();
        soeknad.arbeidPaaLand.fysiskeArbeidssteder.add(fysiskArbeidssted);

        soeknad.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
        soeknad.soeknadsland = Soeknadsland.av(List.of("SE"));

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknad);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        behandling.setSaksopplysninger(saksopplysninger);
        return behandling;
    }
}