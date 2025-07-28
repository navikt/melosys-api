package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.MaritimtArbeid;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LandvelgerServiceTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MottatteOpplysningerService mottatteOpplysningerService;

    private static final long behandlingID = 1;

    private Soeknad søknad;
    private Lovvalgsperiode lovvalgsperiode;
    private Anmodningsperiode anmodningsperiode;
    private LandvelgerService landvelgerService;
    private Behandling behandling;

    private final Land_iso2 søknadsland = Land_iso2.DE;
    private final Land_iso2 avklartArbeidsland = Land_iso2.DK;
    private final Land_iso2 oppgittbostedsland = Land_iso2.SE;
    private final Bostedsland avklartBostedsland = new Bostedsland(Landkoder.FI);
    private final Land_iso2 territorialfarvannLand = Land_iso2.GB;

    @BeforeEach
    public void setUp() {
        søknad = new Soeknad();
        søknad.oppholdUtland.getOppholdslandkoder().add("NO");
        søknad.bosted.getOppgittAdresse().setLandkode(oppgittbostedsland.getKode());
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.setTerritorialfarvannLandkode(territorialfarvannLand.getKode());
        søknad.maritimtArbeid.add(maritimtArbeid);

        lovvalgsperiode = new Lovvalgsperiode();

        anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setUnntakFraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        behandling = lagBehandlingMedSedDokument();

        lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        landvelgerService = new LandvelgerService(avklartefaktaService, behandlingsresultatService, mottatteOpplysningerService);
    }

    private Behandlingsresultat lagBehandlingsresultat(PeriodeOmLovvalg periode) {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
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

        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        return behandlingsresultat;
    }

    private void leggTilAlleAvklartArbeidsland(Collection<Land_iso2> landkoder) {
        for (Land_iso2 landkode : landkoder) {
            søknad.soeknadsland.getLandkoder().add(landkode.getKode());
        }
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(landkoder));
    }

    @Test
    void hentArbeidsland_utenAvklartArbeidsland_girSøknadsland() {
        mockMottatteOpplysninger();
        søknad.soeknadsland.getLandkoder().add(søknadsland.getKode());

        String land = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();

        assertThat(land).isEqualTo(søknadsland.getBeskrivelse());
    }

    @Test
    void hentAlleArbeidsland_medAvklartArbeidsland_girAvklartArbeidsland() {
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Collections.singleton(avklartArbeidsland));

        Collection<Land_iso2> land = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    void hentAlleArbeidsland_medAvklartArbeidslandOgSøknadsland_girAlleUnikeArbeidsland() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Land_iso2.DK, Land_iso2.NO)));
        søknad.soeknadsland.setLandkoder(Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode()));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        Collection<Land_iso2> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(arbeidsland).containsExactlyInAnyOrder(Land_iso2.NO, Land_iso2.DK, Land_iso2.SE).containsOnlyOnce(Land_iso2.DK);
    }

    @Test
    void hentAlleArbeidsland_noenMedMarginaltArbeid_girKunArbeidslandMedVesentligVirksomhet() {
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Arrays.asList(Land_iso2.DK, Land_iso2.SE));
        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(anyLong())).thenReturn(new HashSet<>(Collections.singletonList(Land_iso2.SE)));

        Collection<Land_iso2> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(arbeidsland).containsExactlyInAnyOrder(Land_iso2.DK);
    }

    @Test
    void hentAlleArbeidsland_medArtikkel11_4_2AvklartArbeidslandOgSøknadsland_girKunArbeidsland() {
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Land_iso2.DK, Land_iso2.NO)));
        søknad.soeknadsland.setLandkoder(Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode()));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2);

        Collection<Land_iso2> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(arbeidsland).containsExactlyInAnyOrder(Land_iso2.NO, Land_iso2.DK);
    }

    @Test
    void hentAlleArbeidsland_returnererLovvalgslandKode_nårBehandlingErAnmodningOmUnntak() {
        mockMottatteOpplysninger();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        Collection<Land_iso2> arbeidsland = landvelgerService.hentAlleArbeidsland(behandlingID);

        assertThat(arbeidsland).containsExactly(Land_iso2.BE);
    }

    @Test
    void hentAlleArbeidsland_returnererSøknadslandskoder_dersomSøknadslandHarLandkoder() {
        lagBehandlingsresultat(lovvalgsperiode);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>(Arrays.asList(Land_iso2.DK, Land_iso2.NO)));
        søknad.soeknadsland.setLandkoder(Arrays.asList(Landkoder.DK.getKode(), Landkoder.SE.getKode()));
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        Collection<Land_iso2> arbeidsland = landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingID);

        assertThat(arbeidsland).containsExactlyInAnyOrder(Land_iso2.NO, Land_iso2.DK);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt121_girSøknadsland() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.getLandkoder().add(søknadsland.getKode());
        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt121AvklartArbeidsland_girAvklartArbeidsland() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        leggTilAlleAvklartArbeidsland(Collections.singletonList(avklartArbeidsland));
        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(avklartArbeidsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt122_girSøknadsland() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.getLandkoder().add(søknadsland.getKode());

        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt161_girSøknadsland() {
        mockMottatteOpplysninger();
        Behandlingsresultat resultat = lagBehandlingsresultat(anmodningsperiode);
        resultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);

        søknad.soeknadsland.getLandkoder().add(søknadsland.getKode());
        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt1142_girSøknadsland() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        søknad.soeknadsland.getLandkoder().add(søknadsland.getKode());

        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113A_girOppgittBostedsland() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);

        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).containsExactly(oppgittbostedsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113AOgAvklartBosted_overstyrerOppgittBosted() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(land).containsExactly(Land_iso2.valueOf(avklartBostedsland.landkode()));
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt113AUtenOppgittEllerAvkartBostedsland_girTomListe() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1);

        søknad.bosted.getOppgittAdresse().setLandkode(null);

        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);
        assertThat(land).isEmpty();
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt13BostedsadresseIkkeNorge() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        when(avklartefaktaService.hentAlleAvklarteArbeidsland(anyLong())).thenReturn(new HashSet<>() {{
            add(avklartArbeidsland);
        }});
        søknad.soeknadsland.getLandkoder().add(søknadsland.getKode());

        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(land).containsExactlyInAnyOrder(søknadsland, avklartArbeidsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt13BostedsadresseNorge_girSøknadsland() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        søknad.soeknadsland.getLandkoder().add(søknadsland.getKode());

        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_medArt13Videresending() {
        mockMottatteOpplysninger();
        Fagsak fagsak = FagsakTestFactory.builder().status(Saksstatuser.VIDERESENDT).build();
        Behandling behandling = BehandlingTestFactory.builderWithDefaults().build();
        behandling.setFagsak(fagsak);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setBehandling(behandling);
        behandlingsresultat.setId(behandlingID);
        søknad.foretakUtland = List.of(lagForetakUtland(Landkoder.FR));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(new Bostedsland(Landkoder.DE)));
        søknad.soeknadsland.setLandkoder(List.of(Landkoder.DE.getKode(), Landkoder.FR.getKode()));

        Collection<Land_iso2> land = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(land).containsExactly(søknadsland);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel13IngenArbeidssted_forventLand() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);
        søknad.arbeidPaaLand.setFysiskeArbeidssteder(Collections.emptyList());
        søknad.foretakUtland = Collections.emptyList();
        søknad.soeknadsland.setLandkoder(List.of(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString()));

        Collection<Land_iso2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Land_iso2.NO)
            .contains(Land_iso2.SE, Land_iso2.DK);
        verify(behandlingsresultatService).hentBehandlingsresultat(behandlingID);
        verify(mottatteOpplysningerService, times(3)).hentMottatteOpplysninger(behandlingID);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel13MedArbeidssted_forventLand() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidPaaLand.setFysiskeArbeidssteder(List.of(lagFysiskArbeidssted()));
        søknad.foretakUtland = List.of(lagForetakUtland(Landkoder.ES));
        søknad.soeknadsland.setLandkoder(List.of(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString()));

        Collection<Land_iso2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Land_iso2.NO, Land_iso2.DE, Land_iso2.ES)
            .containsExactlyInAnyOrder(Land_iso2.SE, Land_iso2.DK);

        verify(behandlingsresultatService).hentBehandlingsresultat(behandlingID);
        verify(mottatteOpplysningerService, times(3)).hentMottatteOpplysninger(behandlingID);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel13MedArbeidsstedOgMarginaltArbeid_forventLand() {
        mockMottatteOpplysninger();
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A);

        søknad.arbeidPaaLand.setFysiskeArbeidssteder(List.of(lagFysiskArbeidssted()));
        søknad.foretakUtland = List.of(lagForetakUtland(Landkoder.ES));
        søknad.soeknadsland.setLandkoder(List.of(Landkoder.SE.toString(), Landkoder.DK.toString(), Landkoder.NO.toString()));

        when(avklartefaktaService.hentLandkoderMedMarginaltArbeid(behandlingID))
            .thenReturn(Set.of(Land_iso2.DK, Land_iso2.ES));

        Collection<Land_iso2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland)
            .isNotEmpty()
            .doesNotContain(Land_iso2.NO, Land_iso2.DK, Land_iso2.DE)
            .containsExactlyInAnyOrder(Land_iso2.SE, Land_iso2.ES);

        verify(behandlingsresultatService).hentBehandlingsresultat(behandlingID);
        verify(mottatteOpplysningerService, times(3)).hentMottatteOpplysninger(behandlingID);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel11_5DanmarkValgtAvSaksbehandler_forventEttLandDanmark() {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        when(avklartefaktaService.hentInformertMyndighet(behandlingID)).thenReturn(Optional.of(Land_iso2.DK));

        Collection<Land_iso2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland).containsExactly(Land_iso2.DK);
    }

    @Test
    void hentUtenlandskTrygdemyndighetsland_artikkel11_5SaksbehandlerIkkeValgLand_forventTomListe() {
        lagBehandlingsresultat(lovvalgsperiode);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        when(avklartefaktaService.hentInformertMyndighet(behandlingID)).thenReturn(Optional.empty());

        Collection<Land_iso2> utenlandskeTrygdemyndighetsland = landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID);

        assertThat(utenlandskeTrygdemyndighetsland).isEmpty();
    }

    private static StrukturertAdresse lagUtenlandskAdresse(Landkoder landkode) {
        StrukturertAdresse utenlandskAdresse = new StrukturertAdresse();
        utenlandskAdresse.setLandkode(landkode.toString());
        return utenlandskAdresse;
    }

    private static FysiskArbeidssted lagFysiskArbeidssted() {
        return new FysiskArbeidssted(null, lagUtenlandskAdresse(Landkoder.DE));
    }

    private static ForetakUtland lagForetakUtland(Landkoder landkode) {
        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.setAdresse(lagUtenlandskAdresse(landkode));
        return foretakUtland;
    }

    private void mockMottatteOpplysninger() {
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(søknad);
        mottatteOpplysninger.setBehandling(behandling);
        when(mottatteOpplysningerService.hentMottatteOpplysninger(behandlingID)).thenReturn(mottatteOpplysninger);
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
