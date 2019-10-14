package no.nav.melosys.service.dokument;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LandvelgerServiceTest {
    @Mock
    AvklartefaktaService avklartefaktaService;
    @Mock
    BehandlingsresultatService behandlingsresultatService;
    @Mock
    SoeknadService soeknadService;
    @Mock
    VilkaarsresultatRepository vilkaarsresultatRepository;

    private static final long behandlingID = 1;

    private SoeknadDokument søknad;
    private Lovvalgsperiode lovvalgsperiode;
    private Anmodningsperiode anmodningsperiode;
    private LandvelgerService landvelgerService;
    private List<Vilkaarsresultat> vilkaar = new ArrayList<>();

    private Landkoder søknadsland = Landkoder.DE;
    private Landkoder avklartArbeidsland = Landkoder.DK;
    private Landkoder oppgittbostedsland = Landkoder.SE;
    private Landkoder avklartBostedsland = Landkoder.FI;
    private Landkoder territorialfarvannLand = Landkoder.GB;

    @Before
    public void setUp() throws IkkeFunnetException {
        søknad = new SoeknadDokument();
        Saksopplysning soeknad = new Saksopplysning();
        soeknad.setDokument(søknad);
        soeknad.setType(SaksopplysningType.SØKNAD);
        søknad.oppholdUtland.oppholdslandkoder.add("NO");
        søknad.bosted.oppgittAdresse.landkode = oppgittbostedsland.getKode();
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.territorialfarvann = territorialfarvannLand.getKode();
        søknad.maritimtArbeid.add(maritimtArbeid);

        lovvalgsperiode = new Lovvalgsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(vilkaarsresultatRepository.findByBehandlingsresultatId(anyLong())).thenReturn(vilkaar);
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setSaksopplysninger(new HashSet<>(Collections.singletonList(soeknad)));
        when(soeknadService.hentSøknad(eq(behandlingID))).thenReturn(søknad);

        anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setUnntakFraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        landvelgerService = new LandvelgerService(avklartefaktaService, behandlingsresultatService, soeknadService, vilkaarsresultatRepository);
    }

    private Behandlingsresultat lagBehandlingsresultat(Medlemskapsperiode periode) throws IkkeFunnetException {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        if (periode instanceof Lovvalgsperiode) {
            behandlingsresultat.setLovvalgsperioder(Collections.singleton((Lovvalgsperiode) periode));
        } else if (periode instanceof Anmodningsperiode) {
            behandlingsresultat.setAnmodningsperioder(Collections.singleton((Anmodningsperiode) periode));
        }

        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID))).thenReturn(behandlingsresultat);
        return behandlingsresultat;
    }

    private void oppfyll(Vilkaar vilkaarType) {
        Vilkaarsresultat resultat = new Vilkaarsresultat();
        resultat.setVilkaar(vilkaarType);
        resultat.setOppfylt(true);
        vilkaar.add(resultat);
    }

    private void leggTilAlleAvklartArbeidsland(Collection<Landkoder> landkoder) {
        for (Landkoder landkode : landkoder) {
            søknad.soeknadsland.landkoder.add(landkode.getKode());
        }
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(landkoder));
    }

    @Test
    public void hentArbeidsland_utenAvklartArbeidsland_girSøknadsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);

        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        String land = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();
        assertThat(land).isEqualTo(søknadsland.getBeskrivelse());
    }

    @Test
    public void hentAlleArbeidsland_medAvklartArbeidsland_girAvklartArbeidsland() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Collections.singleton(avklartArbeidsland));

        Collection<Landkoder> land = landvelgerService.hentAlleArbeidsland(behandlingID);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    public void hentAlleArbeidsland_medAvklartArbeidslandOgSøknadsland_girAlleUnikeArbeidsland() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidsland(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK, Landkoder.SE);
        assertThat(arbeidsland).containsOnlyOnce(Landkoder.DK);
    }

    @Test
    public void hentAlleArbeidsland_noenMedMarginaltArbeid_girKunArbeidslandMedVesentligVirksomhet() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Arrays.asList(Landkoder.DK, Landkoder.SE));
        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.SE)));

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidsland(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.DK);
    }

    @Test
    public void hentAlleArbeidsland_medArtikkel11_4_2AvklartArbeidslandOgSøknadsland_girKunArbeidsland() throws FunksjonellException {
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidsland(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt121_girSøknadsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART12_1);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt121AvklartArbeidsland_girAvklartArbeidsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART12_1);
        leggTilAlleAvklartArbeidsland(Arrays.asList(avklartArbeidsland));
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt122_girSøknadsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART12_2);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt161_girSøknadsland() throws Exception {
        Behandlingsresultat resultat = lagBehandlingsresultat(anmodningsperiode);
        resultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        oppfyll(Vilkaar.FO_883_2004_ART16_1);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt1142_girSøknadsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_2);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113A_girOppgittBostedsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(oppgittbostedsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113AOgAvklartBosted_overstyrerOppgittBosted() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartBostedsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113AUtenOppgittEllerAvkartBostedsland_girNorge() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);

        søknad.bosted.oppgittAdresse.landkode = null;

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(Landkoder.NO);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt13BostedsadresseIkkeNorge() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartBostedsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt13BostedsadresseNorge() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(Landkoder.NO));

        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }
}