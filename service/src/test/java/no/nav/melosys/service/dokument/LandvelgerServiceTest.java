package no.nav.melosys.service.dokument;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LandvelgerServiceTest {

    @Mock
    AvklartefaktaService avklartefaktaService;

    @Mock
    VilkaarsresultatRepository vilkaarsresultatRepository;

    @Mock
    BehandlingsresultatService behandlingsresultatService;
    
    @Mock
    Behandling behandling;

    Lovvalgsperiode lovvalgsperiode;
    
    private SoeknadDokument søknad;
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
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Collections.singletonList(soeknad)));

        landvelgerService = new LandvelgerService(avklartefaktaService, vilkaarsresultatRepository, behandlingsresultatService);
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
    public void hentArbeidsland_utenAvklartArbeidsland_girSøknadsland() throws TekniskException, FunksjonellException {
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        String land = landvelgerService.hentArbeidsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(søknadsland.getBeskrivelse());
    }

    @Test
    public void hentAlleArbeidsland_medAvklartArbeidsland_girAvklartArbeidsland() throws TekniskException {
        leggTilAlleAvklartArbeidsland(Collections.singleton(avklartArbeidsland));

        Collection<Landkoder> land = landvelgerService.hentAlleArbeidsland(behandling);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    public void hentAlleArbeidsland_medAvklartArbeidslandOgSøknadsland_girAlleUnikeArbeidsland() throws TekniskException {
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidsland(behandling);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK, Landkoder.SE);
        assertThat(arbeidsland).containsOnlyOnce(Landkoder.DK);
    }

    @Test
    public void hentAlleArbeidsland_noenMedMarginaltArbeid_girKunArbeidslandMedVesentligVirksomhet() throws TekniskException {
        leggTilAlleAvklartArbeidsland(Arrays.asList(Landkoder.DK, Landkoder.SE));
        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.SE)));

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidsland(behandling);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.DK);
    }

    @Test
    public void hentAlleArbeidsland_medArtikkel11_4_2AvklartArbeidslandOgSøknadsland_girKunArbeidsland() throws TekniskException {
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidsland(behandling);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK);
    }

    @Test
    public void hentAlleArbeidsland_medArtikkel12_1AvklartArbeidslandOgSøknadsland_girKunArbeidsland() throws TekniskException {
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidsland(behandling);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt121_girSøknadsland() throws TekniskException, FunksjonellException {
        oppfyll(Vilkaar.FO_883_2004_ART12_1);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt121AvklartArbeidsland_girAvklartArbeidsland() throws TekniskException, FunksjonellException {
        oppfyll(Vilkaar.FO_883_2004_ART12_1);
        leggTilAlleAvklartArbeidsland(Arrays.asList(avklartArbeidsland));
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt122_girSøknadsland() throws TekniskException, FunksjonellException {
        oppfyll(Vilkaar.FO_883_2004_ART12_2);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt161_girSøknadsland() throws TekniskException, FunksjonellException {
        oppfyll(Vilkaar.FO_883_2004_ART16_1);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt1142_girSøknadsland() throws TekniskException, FunksjonellException {
        oppfyll(Vilkaar.FO_883_2004_ART11_4_2);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113A_girOppgittBostedsland() throws TekniskException, FunksjonellException {
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        assertThat(land).containsExactly(oppgittbostedsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113AOgAvklartBosted_overstyrerOppgittBosted() throws TekniskException, FunksjonellException {
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        assertThat(land).containsExactly(avklartBostedsland);
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113AUtenOppgittEllerAvkartBostedsland_girNorge() throws TekniskException, FunksjonellException {
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);

        søknad.bosted.oppgittAdresse.landkode = null;

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling);
        assertThat(land).containsExactly(Landkoder.NO);
    }
}