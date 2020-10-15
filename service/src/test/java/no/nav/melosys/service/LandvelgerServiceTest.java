package no.nav.melosys.service;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.ForetakUtland;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LandvelgerServiceTest {
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private VilkaarsresultatRepository vilkaarsresultatRepository;

    private static final long behandlingID = 1;

    private Soeknad søknad;
    private Lovvalgsperiode lovvalgsperiode;
    private Anmodningsperiode anmodningsperiode;
    private LandvelgerService landvelgerService;
    private final List<Vilkaarsresultat> vilkaar = new ArrayList<>();

    private final Landkoder søknadsland = Landkoder.DE;
    private final Landkoder avklartArbeidsland = Landkoder.DK;
    private final Landkoder oppgittbostedsland = Landkoder.SE;
    private final Landkoder avklartBostedsland = Landkoder.FI;
    private final Landkoder territorialfarvannLand = Landkoder.GB;

    @Before
    public void setUp() throws IkkeFunnetException {
        søknad = new Soeknad();
        søknad.oppholdUtland.oppholdslandkoder.add("NO");
        søknad.bosted.oppgittAdresse.landkode = oppgittbostedsland.getKode();
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.territorialfarvann = territorialfarvannLand.getKode();
        søknad.maritimtArbeid.add(maritimtArbeid);

        lovvalgsperiode = new Lovvalgsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        behandlingsresultat.setId(behandlingID);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        when(vilkaarsresultatRepository.findByBehandlingsresultatId(anyLong())).thenReturn(vilkaar);
        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknad);
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(eq(behandlingID))).thenReturn(behandlingsgrunnlag);

        anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setUnntakFraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        landvelgerService = new LandvelgerService(avklartefaktaService, behandlingsresultatService, behandlingsgrunnlagService, vilkaarsresultatRepository);
    }

    private Behandlingsresultat lagBehandlingsresultat(Medlemskapsperiode periode) throws IkkeFunnetException {
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);
        behandling.setFagsak(fagsak);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setId(behandlingID);
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

        Collection<Landkoder> land = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    public void hentAlleArbeidsland_medAvklartArbeidslandOgSøknadsland_girAlleUnikeArbeidsland() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK, Landkoder.SE).containsOnlyOnce(Landkoder.DK);
    }

    @Test
    public void hentAlleArbeidsland_noenMedMarginaltArbeid_girKunArbeidslandMedVesentligVirksomhet() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Arrays.asList(Landkoder.DK, Landkoder.SE));
        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(anyLong())).thenReturn(new HashSet<>(Collections.singletonList(Landkoder.SE)));

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.DK);
    }

    @Test
    public void hentAlleArbeidsland_medArtikkel11_4_2AvklartArbeidslandOgSøknadsland_girKunArbeidsland() throws FunksjonellException {
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK);
    }

    @Test
    public void hentAlleArbeidsland_medArt161_girSøknadsland() throws Exception {
        Behandlingsresultat resultat = lagBehandlingsresultat(anmodningsperiode);
        resultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        oppfyll(Vilkaar.FO_883_2004_ART16_1);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt121_girSøknadsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART12_1);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt121AvklartArbeidsland_girAvklartArbeidsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART12_1);
        leggTilAlleAvklartArbeidsland(Collections.singletonList(avklartArbeidsland));
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt122_girSøknadsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART12_2);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt161_girSøknadsland() throws Exception {
        Behandlingsresultat resultat = lagBehandlingsresultat(anmodningsperiode);
        resultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        oppfyll(Vilkaar.FO_883_2004_ART16_1);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt1142_girSøknadsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_2);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt113A_girOppgittBostedsland() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(oppgittbostedsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt113AOgAvklartBosted_overstyrerOppgittBosted() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartBostedsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt113AUtenOppgittEllerAvkartBostedsland_girTomListe() throws FunksjonellException {
        lagBehandlingsresultat(lovvalgsperiode);
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);

        søknad.bosted.oppgittAdresse.landkode = null;

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).isEmpty();
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt13BostedsadresseIkkeNorge() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>() {{
            add(avklartBostedsland);
        }});

        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactlyInAnyOrder(søknadsland, avklartBostedsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt13BostedsadresseNorge_girSøknadsland() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_medArt13Videresending() throws IkkeFunnetException {
        Fagsak fagsak = new Fagsak();
        fagsak.setStatus(Saksstatuser.VIDERESENDT);
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setId(behandlingID);

        søknad.foretakUtland = List.of(lagForetakUtland(Landkoder.FR));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(Landkoder.DE));

        søknad.soeknadsland.landkoder = List.of(Landkoder.DE.getKode(), Landkoder.FR.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_artikkel13IngenArbeidUtland_forventLand() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidUtland = Collections.emptyList();
        søknad.foretakUtland = Collections.emptyList();
        søknad.soeknadsland.landkoder = List.of(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString());

        Collection<Landkoder> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Landkoder.NO)
            .contains(Landkoder.SE, Landkoder.DK);

        verify(behandlingsresultatService).hentBehandlingsresultat(eq(behandlingID));
        verify(behandlingsgrunnlagService, times(3)).hentBehandlingsgrunnlag(eq(behandlingID));
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_artikkel13MedArbeidUtland_forventLand() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidUtland = List.of(lagArbeidUtland(Landkoder.DE));
        søknad.foretakUtland = List.of(lagForetakUtland(Landkoder.ES));
        søknad.soeknadsland.landkoder = List.of(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString());

        Collection<Landkoder> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Landkoder.NO, Landkoder.DE, Landkoder.ES)
            .containsExactlyInAnyOrder(Landkoder.SE, Landkoder.DK);

        verify(behandlingsresultatService).hentBehandlingsresultat(eq(behandlingID));
        verify(behandlingsgrunnlagService, times(3)).hentBehandlingsgrunnlag(eq(behandlingID));
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_artikkel13MedArbeidUtlandOgMarginaltArbeid_forventLand() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidUtland = List.of(lagArbeidUtland(Landkoder.DE));
        søknad.foretakUtland = List.of(lagForetakUtland(Landkoder.ES));
        søknad.soeknadsland.landkoder = List.of(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString());

        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(eq(behandlingID)))
            .thenReturn(Set.of(Landkoder.DK, Landkoder.ES));

        Collection<Landkoder> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Landkoder.NO, Landkoder.DK, Landkoder.DE)
            .containsExactlyInAnyOrder(Landkoder.SE, Landkoder.ES);

        verify(behandlingsresultatService).hentBehandlingsresultat(eq(behandlingID));
        verify(behandlingsgrunnlagService, times(3)).hentBehandlingsgrunnlag(eq(behandlingID));
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_artikkel11_5DanmarkValgtAvSaksbehandler_forventEttLandDanmark() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        when(avklartefaktaService.hentInformertMyndighet(eq(behandlingID))).thenReturn(Optional.of(Landkoder.DK));

        Collection<Landkoder> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland).containsExactly(Landkoder.DK);
    }

    @Test
    public void hentUtenlandskTrygdemyndighetsland_artikkel11_5SaksbehandlerIkkeValgLand_forventTomListe() throws IkkeFunnetException {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        when(avklartefaktaService.hentInformertMyndighet(eq(behandlingID))).thenReturn(Optional.empty());

        Collection<Landkoder> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland).isEmpty();
    }

    private static StrukturertAdresse lagUtenlandskAdresse(Landkoder landkode) {
        StrukturertAdresse utenlandskAdresse = new StrukturertAdresse();
        utenlandskAdresse.landkode = landkode.toString();
        return utenlandskAdresse;
    }

    private static ArbeidUtland lagArbeidUtland(Landkoder landkode) {
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = lagUtenlandskAdresse(landkode);
        return arbeidUtland;
    }

    private static ForetakUtland lagForetakUtland(Landkoder landkode) {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.adresse = lagUtenlandskAdresse(landkode);
        return foretakUtland;
    }
}