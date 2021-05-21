package no.nav.melosys.service;

import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandvelgerServiceTest {
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private static final long behandlingID = 1;

    private Soeknad søknad;
    private Lovvalgsperiode lovvalgsperiode;
    private Anmodningsperiode anmodningsperiode;
    private LandvelgerService landvelgerService;

    private final Landkoder søknadsland = Landkoder.DE;
    private final Landkoder avklartArbeidsland = Landkoder.DK;
    private final Landkoder oppgittbostedsland = Landkoder.SE;
    private final Landkoder avklartBostedsland = Landkoder.FI;
    private final Landkoder territorialfarvannLand = Landkoder.GB;

    @BeforeEach
    public void setUp() {
        søknad = new Soeknad();
        søknad.oppholdUtland.oppholdslandkoder.add("NO");
        søknad.bosted.oppgittAdresse.landkode = oppgittbostedsland.getKode();
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.territorialfarvann = territorialfarvannLand.getKode();
        søknad.maritimtArbeid.add(maritimtArbeid);

        lovvalgsperiode = new Lovvalgsperiode();

        anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setUnntakFraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        landvelgerService = new LandvelgerService(avklartefaktaService, behandlingsresultatService, behandlingsgrunnlagService);
    }

    private Behandlingsresultat lagBehandlingsresultat(PeriodeOmLovvalg periode) {
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

    private void leggTilAlleAvklartArbeidsland(Collection<Landkoder> landkoder) {
        for (Landkoder landkode : landkoder) {
            søknad.soeknadsland.landkoder.add(landkode.getKode());
        }
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(landkoder));
    }

    @Test
    void hentArbeidsland_utenAvklartArbeidsland_girSøknadsland() {
        mockBehandlingsgrunnlag();

        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        String land = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();
        assertThat(land).isEqualTo(søknadsland.getBeskrivelse());
    }

    @Test
    void hentAlleArbeidsland_medAvklartArbeidsland_girAvklartArbeidsland() {
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Collections.singleton(avklartArbeidsland));

        Collection<Landkoder> land = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    void hentAlleArbeidsland_medAvklartArbeidslandOgSøknadsland_girAlleUnikeArbeidsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK, Landkoder.SE).containsOnlyOnce(Landkoder.DK);
    }

    @Test
    void hentAlleArbeidsland_noenMedMarginaltArbeid_girKunArbeidslandMedVesentligVirksomhet() {
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Arrays.asList(Landkoder.DK, Landkoder.SE));
        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(anyLong())).thenReturn(new HashSet<>(Collections.singletonList(Landkoder.SE)));

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.DK);
    }

    @Test
    void hentAlleArbeidsland_medArtikkel11_4_2AvklartArbeidslandOgSøknadsland_girKunArbeidsland() {
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Landkoder.DK, Landkoder.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);

        Collection<Landkoder> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);
        assertThat(arbeidsland).containsExactlyInAnyOrder(Landkoder.NO, Landkoder.DK);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt121_girSøknadsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt121AvklartArbeidsland_girAvklartArbeidsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Collections.singletonList(avklartArbeidsland));
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt122_girSøknadsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt161_girSøknadsland() throws Exception {
        mockBehandlingsgrunnlag();
        Behandlingsresultat resultat = lagBehandlingsresultat(anmodningsperiode);
        resultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt1142_girSøknadsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113A_girOppgittBostedsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(oppgittbostedsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113AOgAvklartBosted_overstyrerOppgittBosted() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartBostedsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113AUtenOppgittEllerAvkartBostedsland_girTomListe() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);

        søknad.bosted.oppgittAdresse.landkode = null;

        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).isEmpty();
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt13BostedsadresseIkkeNorge() {
        mockBehandlingsgrunnlag();
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
    void hentUtenlandskTrygdemyndighetsland_medArt13BostedsadresseNorge_girSøknadsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Landkoder> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt13Videresending() {
        mockBehandlingsgrunnlag();
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
    void hentUtenlandskTrygdemyndighetsland_artikkel13IngenArbeidssted_forventLand() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidPaaLand.fysiskeArbeidssteder = Collections.emptyList();
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
    void hentUtenlandskTrygdemyndighetsland_artikkel13MedArbeidssted_forventLand() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidPaaLand.fysiskeArbeidssteder = List.of(lagFysiskArbeidssted(Landkoder.DE));
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
    void hentUtenlandskTrygdemyndighetsland_artikkel13MedArbeidsstedOgMarginaltArbeid_forventLand() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidPaaLand.fysiskeArbeidssteder = List.of(lagFysiskArbeidssted(Landkoder.DE));
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
    void hentUtenlandskTrygdemyndighetsland_artikkel11_5DanmarkValgtAvSaksbehandler_forventEttLandDanmark() {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        when(avklartefaktaService.hentInformertMyndighet(eq(behandlingID))).thenReturn(Optional.of(Landkoder.DK));

        Collection<Landkoder> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland).containsExactly(Landkoder.DK);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel11_5SaksbehandlerIkkeValgLand_forventTomListe() {
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

    private static FysiskArbeidssted lagFysiskArbeidssted(Landkoder landkode) {
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse = lagUtenlandskAdresse(landkode);
        return fysiskArbeidssted;
    }

    private static ForetakUtland lagForetakUtland(Landkoder landkode) {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.adresse = lagUtenlandskAdresse(landkode);
        return foretakUtland;
    }

    private void mockBehandlingsgrunnlag() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknad);
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(eq(behandlingID))).thenReturn(behandlingsgrunnlag);
    }
}
