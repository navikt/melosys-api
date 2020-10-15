package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.dokument.soeknad.Soeknadsland;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.KontrollresultatService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppfriskSaksopplysningerServiceTest {
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
    private TpsFasade tpsFasade;

    private OppfriskSaksopplysningerService oppfriskSaksopplysningerService;

    private static final long BEHANDLING_ID = 11L;

    @Before
    public void setUp() throws IkkeFunnetException {
        oppfriskSaksopplysningerService = new OppfriskSaksopplysningerService(
            behandlingService, behandlingsresultatService,
            fagsakService, kontrollresultatService,
            inngangsvilkaarService, registeropplysningerService,
            tpsFasade);

        String brukerID = "322211";
        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn(brukerID);
    }

    @Test
    public void oppfriskSaksopplysning() throws MelosysException {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID);

        verify(behandlingsresultatService).tømBehandlingsresultat(anyLong());
        verify(registeropplysningerService).hentOgLagreOpplysninger(any(RegisteropplysningerRequest.class));
    }

    @Test
    public void oppfriskSaksopplysning_medSED_kallerKontroller() throws MelosysException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE);

        behandling.getSaksopplysninger().add(lagSED());
        when(behandlingService.hentBehandling(eq(BEHANDLING_ID))).thenReturn(behandling);

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID);

        verify(kontrollresultatService).utførKontrollerOgRegistrerFeil(eq(BEHANDLING_ID));
    }

    @Test
    public void oppfriskSaksopplysning_sakstypeUkjentErSøknad_oppdatererType() throws MelosysException {
        Behandling behandling = lagBehandling();
        behandling.getFagsak().setType(Sakstyper.UKJENT);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(anyLong(), anyList(), any(Periode.class))).thenReturn(true);

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID);

        verify(fagsakService).oppdaterType(eq(behandling.getFagsak()), eq(true));
        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(eq(behandling.getId()), eq(List.of("SE")), any(Periode.class));
    }

    @Test
    public void oppfriskSaksopplysning_sakstypeUkjentNorgeUtpekt_oppdatererType() throws MelosysException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandling.getFagsak().setType(Sakstyper.UKJENT);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(inngangsvilkaarService.vurderOgLagreInngangsvilkår(anyLong(), anyList(), any(Periode.class))).thenReturn(true);

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(BEHANDLING_ID);

        verify(fagsakService).oppdaterType(eq(behandling.getFagsak()), eq(true));
        verify(inngangsvilkaarService).vurderOgLagreInngangsvilkår(eq(behandling.getId()), eq(List.of("NO")), any(Periode.class));
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

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        soeknad.arbeidUtland = new ArrayList<>();
        soeknad.arbeidUtland.add(arbeidUtland);

        soeknad.periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(2));
        soeknad.soeknadsland = Soeknadsland.av(List.of("SE"));

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknad);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        behandling.setSaksopplysninger(saksopplysninger);
        return behandling;
    }
}