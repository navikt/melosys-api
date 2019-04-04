package no.nav.melosys.saksflyt.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.saksflyt.agent.iv.AvklarMyndighet;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AvklarMyndighetTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;

    @Mock
    private VilkaarsresultatRepository vilkaarsresultatRepository;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private FagsakService fagsakService;


    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    private LandvelgerService landvelgerService;

    private AvklarMyndighet steg;

    private Prosessinstans p;

    private List<Vilkaarsresultat> vilkaar = new ArrayList<>();


    @Before
    public void setUp() {
        landvelgerService = new LandvelgerService(avklartefaktaService, vilkaarsresultatRepository);
        steg = new AvklarMyndighet(behandlingRepository, behandlingsresultatRepository, fagsakService, landvelgerService, utenlandskMyndighetRepository);

        p = new Prosessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");

        Behandling behandling = lagBehandling(fagsak);
        when(behandlingRepository.findWithSaksopplysningerById(anyLong())).thenReturn(behandling);
        p.setBehandling(behandling);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        when(behandlingsresultatRepository.findWithSaksbehandlingById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));

        when(vilkaarsresultatRepository.findByBehandlingsresultatId(anyLong())).thenReturn(vilkaar);
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.SOEKNAD);
        SoeknadDokument søknadDokument = new SoeknadDokument();
        søknadDokument.soeknadsland.landkoder.add("BE");
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse.landkode = "HR";
        søknadDokument.arbeidUtland.add(arbeidUtland);
        søknadDokument.bosted.oppgittAdresse.landkode = "IT";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysning.setDokument(søknadDokument);
        behandling.getSaksopplysninger().add(saksopplysning);
        return behandling;
    }

    @Test
    public void utfør_utenMyndighet_myndighetOpprettes() throws FunksjonellException, TekniskException {
        UtenlandskMyndighet utenlandskMyndighet = lagUtenlandskMyndighet();
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.BE))).thenReturn(utenlandskMyndighet);

        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatRepository.findWithSaksbehandlingById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));

        steg.utfør(p);

        String forventetID = utenlandskMyndighet.land + ":" + utenlandskMyndighet.institusjonskode;
        verify(fagsakService).leggTilAktør(eq("saksnr"), eq(Aktoersroller.MYNDIGHET), eq(forventetID));
    }

    private static Behandlingsresultat lagBehandlingResultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        return behandlingsresultat;
    }

    private static UtenlandskMyndighet lagUtenlandskMyndighet() {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.land = "BE";
        utenlandskMyndighet.institusjonskode = "zfga";
        return utenlandskMyndighet;
    }

    @Test
    public void utfør_medMyndighet_myndighetOpprettesIkke() throws FunksjonellException, TekniskException {
        Fagsak fagsakMedMyndighet = new Fagsak();
        fagsakMedMyndighet.setSaksnummer("med myndighet");
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        fagsakMedMyndighet.getAktører().add(myndighet);
        p.getBehandling().setFagsak(fagsakMedMyndighet);

        steg.utfør(p);

        verify(fagsakService, never()).leggTilAktør(any(), any(), any());
    }

    @Test
    public void utfør_iverksettVedtakSjekkSteg_forventAvklarArbeidsgiver() throws Exception {
        UtenlandskMyndighet utenlandskMyndighet = lagUtenlandskMyndighet();
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.BE))).thenReturn(utenlandskMyndighet);

        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatRepository.findWithSaksbehandlingById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.IV_AVKLAR_ARBEIDSGIVER);
    }

    @Test
    public void utfør_anmodningUnntakSjekkSteg_forventAouOppdaterMedl() throws Exception {
        UtenlandskMyndighet utenlandskMyndighet = lagUtenlandskMyndighet();
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.BE))).thenReturn(utenlandskMyndighet);

        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatRepository.findWithSaksbehandlingById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));
        no.nav.melosys.saksflyt.agent.aou.AvklarMyndighet steg =
            new no.nav.melosys.saksflyt.agent.aou.AvklarMyndighet(behandlingRepository, behandlingsresultatRepository, fagsakService, landvelgerService, utenlandskMyndighetRepository);
        steg.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.AOU_OPPDATER_MEDL);
    }
}