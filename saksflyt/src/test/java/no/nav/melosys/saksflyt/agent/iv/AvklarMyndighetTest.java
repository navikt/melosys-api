package no.nav.melosys.saksflyt.agent.iv;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
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
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private FagsakService fagsakService;

    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    private AvklarMyndighet steg;

    private Prosessinstans p;

    @Before
    public void setUp() {
        steg = new AvklarMyndighet(avklartefaktaService, behandlingsresultatRepository, fagsakService, utenlandskMyndighetRepository);

        p = new Prosessinstans();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnr");

        p.setBehandling(lagBehandling(fagsak));
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        when(behandlingsresultatRepository.findById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setFagsak(fagsak);
        SoeknadDokument søknadDokument = new SoeknadDokument();
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse.landKode = "BE";
        søknadDokument.arbeidUtland.add(arbeidUtland);
        søknadDokument.bosted.oppgittAdresse.landKode = "IT";
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        saksopplysning.setDokument(søknadDokument);
        behandling.getSaksopplysninger().add(saksopplysning);
        return behandling;
    }

    @Test
    public void utfør_utenMyndighet_myndighetOpprettes() throws FunksjonellException, TekniskException {
        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.land = "BE";
        utenlandskMyndighet.institusjonskode = "zfga";
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.BE))).thenReturn(utenlandskMyndighet);

        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        when(behandlingsresultatRepository.findById(eq(1L))).thenReturn(Optional.of(behandlingsresultat));

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
    public void avklarLand_arbeidsland() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling(new Fagsak());
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(3L);

        Landkoder landkode = steg.avklarLand(behandling, behandlingsresultat);

        assertThat(landkode).isEqualTo(Landkoder.BE);
    }

    @Test
    public void avklarLand_11_4_1og11_3A__brukerBostedLand() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling(new Fagsak());
        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        Vilkaarsresultat v1 = new Vilkaarsresultat();
        v1.setOppfylt(true);
        v1.setVilkaar(Vilkaar.FO_883_2004_ART11_4_1);
        Vilkaarsresultat v2 = new Vilkaarsresultat();
        v2.setOppfylt(true);
        v2.setVilkaar(Vilkaar.FO_883_2004_ART11_3A);
        behandlingsresultat.getVilkaarsresultater().add(v1);
        behandlingsresultat.getVilkaarsresultater().add(v2);

        Avklartefakta avklartLand = new Avklartefakta();
        avklartLand.setFakta("SE");
        when(avklartefaktaService.hentAvklarteFakta(anyLong(), eq(Avklartefaktatype.BOSTEDSLAND))).thenReturn(avklartLand);

        Landkoder landkode = steg.avklarLand(behandling, behandlingsresultat);

        assertThat(landkode).isEqualTo(Landkoder.SE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void avklarLand_11_4_1_uten_11_3A__brukerFlaggLand() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling(new Fagsak());
        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        Vilkaarsresultat v1 = new Vilkaarsresultat();
        v1.setOppfylt(true);
        v1.setVilkaar(Vilkaar.FO_883_2004_ART11_4_1);
        behandlingsresultat.getVilkaarsresultater().add(v1);

        Avklartefakta avklartLand = new Avklartefakta();
        avklartLand.setFakta("ES");
        //FIXME when(avklartefaktaService.hentAvklarteFakta(anyLong(), eq(Avklartefaktatype.FLAGGLAND))).thenReturn(avklartLand);

        Landkoder landkode = steg.avklarLand(behandling, behandlingsresultat);

        assertThat(landkode).isEqualTo("trenger FLAGGLAND");
    }

    @Test
    public void avklarLand_11_3A_uten_11_4_1_brukerAdresseLand() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling(new Fagsak());
        Behandlingsresultat behandlingsresultat = lagBehandlingResultat();
        Vilkaarsresultat v1 = new Vilkaarsresultat();
        v1.setOppfylt(true);
        v1.setVilkaar(Vilkaar.FO_883_2004_ART11_3A);
        behandlingsresultat.getVilkaarsresultater().add(v1);

        Landkoder landkode = steg.avklarLand(behandling, behandlingsresultat);

        assertThat(landkode).isEqualTo(Landkoder.IT);
    }
}