package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Soeknadsland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.util.Land_ISO2;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
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
    private Behandling behandling;

    private final Land_ISO2 søknadsland = Land_ISO2.DE;
    private final Land_ISO2 avklartArbeidsland = Land_ISO2.DK;
    private final Land_ISO2 oppgittbostedsland = Land_ISO2.SE;
    private final Bostedsland avklartBostedsland = new Bostedsland(Landkoder.FI);
    private final Land_ISO2 territorialfarvannLand = Land_ISO2.GB;

    @BeforeEach
    public void setUp() {
        søknad = new Soeknad();
        søknad.oppholdUtland.oppholdslandkoder.add("NO");
        søknad.bosted.oppgittAdresse.setLandkode(oppgittbostedsland.getKode());
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.territorialfarvannLandkode = territorialfarvannLand.getKode();
        søknad.maritimtArbeid.add(maritimtArbeid);

        lovvalgsperiode = new Lovvalgsperiode();

        anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setUnntakFraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        behandling = lagBehandlingMedSedDokument();

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

    private void leggTilAlleAvklartArbeidsland(Collection<Land_ISO2> landkoder) {
        for (Land_ISO2 landkode : landkoder) {
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

        Collection<Land_ISO2> land = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    void hentAlleArbeidsland_medAvklartArbeidslandOgSøknadsland_girAlleUnikeArbeidsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Land_ISO2.DK, Land_ISO2.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        Collection<Land_ISO2> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(arbeidsland).containsExactlyInAnyOrder(Land_ISO2.NO, Land_ISO2.DK, Land_ISO2.SE).containsOnlyOnce(Land_ISO2.DK);
    }

    @Test
    void hentAlleArbeidsland_noenMedMarginaltArbeid_girKunArbeidslandMedVesentligVirksomhet() {
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Arrays.asList(Land_ISO2.DK, Land_ISO2.SE));
        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(anyLong())).thenReturn(new HashSet<>(Collections.singletonList(Land_ISO2.SE)));

        Collection<Land_ISO2> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(arbeidsland).containsExactlyInAnyOrder(Land_ISO2.DK);
    }

    @Test
    void hentAlleArbeidsland_medArtikkel11_4_2AvklartArbeidslandOgSøknadsland_girKunArbeidsland() {
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Land_ISO2.DK, Land_ISO2.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);

        Collection<Land_ISO2> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(arbeidsland).containsExactlyInAnyOrder(Land_ISO2.NO, Land_ISO2.DK);
    }

    @Test
    void hentAlleArbeidsland_returnererLovvalgslandKode_nårBehandlingErAnmodningOmUnntak() {
        mockBehandlingsgrunnlag();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        Collection<Land_ISO2> arbeidsland = landvelgerService.hentAlleArbeidsland(behandlingID);

        assertThat(arbeidsland).containsExactly(Land_ISO2.BE);
    }

    @Test
    void hentAlleArbeidsland_returnererSøknadslandskoder_dersomSøknadslandHarLandkoder() {
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Land_ISO2.DK, Land_ISO2.NO)));
        søknad.soeknadsland.landkoder = Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode());
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        Collection<Land_ISO2> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(arbeidsland).containsExactlyInAnyOrder(Land_ISO2.NO, Land_ISO2.DK);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt121_girSøknadsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt121AvklartArbeidsland_girAvklartArbeidsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Collections.singletonList(avklartArbeidsland));
        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt122_girSøknadsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt161_girSøknadsland() {
        mockBehandlingsgrunnlag();
        Behandlingsresultat resultat = lagBehandlingsresultat(anmodningsperiode);
        resultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt1142_girSøknadsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113A_girOppgittBostedsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);

        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(oppgittbostedsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113AOgAvklartBosted_overstyrerOppgittBosted() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(land).containsExactly(Land_ISO2.valueOf(avklartBostedsland.landkode()));
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113AUtenOppgittEllerAvkartBostedsland_girTomListe() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);

        søknad.bosted.oppgittAdresse.setLandkode(null);

        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).isEmpty();
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt13BostedsadresseIkkeNorge() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>() {{
            add(avklartArbeidsland);
        }});
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(land).containsExactlyInAnyOrder(søknadsland, avklartArbeidsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt13BostedsadresseNorge_girSøknadsland() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());

        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

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
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(new Bostedsland(Landkoder.DE)));
        søknad.soeknadsland.landkoder = List.of(Landkoder.DE.getKode(), Landkoder.FR.getKode());

        Collection<Land_ISO2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

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

        Collection<Land_ISO2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Land_ISO2.NO)
            .contains(Land_ISO2.SE, Land_ISO2.DK);
        verify(behandlingsresultatService).hentBehandlingsresultat(eq(behandlingID));
        verify(behandlingsgrunnlagService, times(3)).hentBehandlingsgrunnlag(eq(behandlingID));
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel13MedArbeidssted_forventLand() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidPaaLand.fysiskeArbeidssteder = List.of(lagFysiskArbeidssted());
        søknad.foretakUtland = List.of(lagForetakUtland(Landkoder.ES));
        søknad.soeknadsland.landkoder = List.of(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString());

        Collection<Land_ISO2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Land_ISO2.NO, Land_ISO2.DE, Land_ISO2.ES)
            .containsExactlyInAnyOrder(Land_ISO2.SE, Land_ISO2.DK);

        verify(behandlingsresultatService).hentBehandlingsresultat(eq(behandlingID));
        verify(behandlingsgrunnlagService, times(3)).hentBehandlingsgrunnlag(eq(behandlingID));
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel13MedArbeidsstedOgMarginaltArbeid_forventLand() {
        mockBehandlingsgrunnlag();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidPaaLand.fysiskeArbeidssteder = List.of(lagFysiskArbeidssted());
        søknad.foretakUtland = List.of(lagForetakUtland(Landkoder.ES));
        søknad.soeknadsland.landkoder = List.of(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString());

        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(eq(behandlingID)))
            .thenReturn(Set.of(Land_ISO2.DK, Land_ISO2.ES));

        Collection<Land_ISO2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Land_ISO2.NO, Land_ISO2.DK, Land_ISO2.DE)
            .containsExactlyInAnyOrder(Land_ISO2.SE, Land_ISO2.ES);

        verify(behandlingsresultatService).hentBehandlingsresultat(eq(behandlingID));
        verify(behandlingsgrunnlagService, times(3)).hentBehandlingsgrunnlag(eq(behandlingID));
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel11_5DanmarkValgtAvSaksbehandler_forventEttLandDanmark() {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        when(avklartefaktaService.hentInformertMyndighet(eq(behandlingID))).thenReturn(Optional.of(Land_ISO2.DK));

        Collection<Land_ISO2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland).containsExactly(Land_ISO2.DK);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel11_5SaksbehandlerIkkeValgLand_forventTomListe() {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        when(avklartefaktaService.hentInformertMyndighet(eq(behandlingID))).thenReturn(Optional.empty());

        Collection<Land_ISO2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland).isEmpty();
    }

    private static StrukturertAdresse lagUtenlandskAdresse(Landkoder landkode) {
        StrukturertAdresse utenlandskAdresse = new StrukturertAdresse();
        utenlandskAdresse.setLandkode(landkode.toString());
        return utenlandskAdresse;
    }

    private static FysiskArbeidssted lagFysiskArbeidssted() {
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted();
        fysiskArbeidssted.adresse = lagUtenlandskAdresse(Landkoder.DE);
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
        behandlingsgrunnlag.setBehandling(behandling);
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(eq(behandlingID))).thenReturn(behandlingsgrunnlag);
    }

    private Behandling lagBehandlingMedSedDokument() {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setSedType(SedType.A001);
        sedDokument.setUnntakFraLovvalgslandKode(Landkoder.BE);
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now().plusMonths(1)));

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);
        saksopplysning.setType(SaksopplysningType.SEDOPPL);

        Behandling behandling = SaksbehandlingDataFactory.lagBehandling();
        behandling.setTema(Behandlingstema.ARBEID_FLERE_LAND);
        behandling.getSaksopplysninger().add(saksopplysning);
        return behandling;
    }
}
