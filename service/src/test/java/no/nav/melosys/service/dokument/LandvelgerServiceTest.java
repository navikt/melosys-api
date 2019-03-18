package no.nav.melosys.service.dokument;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.TilleggsBestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
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
    LovvalgsperiodeService lovvalgsperiodeService;

    @Mock
    Behandling behandling;

    private SoeknadDokument søknad;
    private Lovvalgsperiode lovvalgsperiode;
    private LandvelgerService landvelgerService;

    private Landkoder oppholdsland = Landkoder.NO;
    private Landkoder flaggland = Landkoder.DK;
    private Landkoder bostedsland = Landkoder.SE;
    private Landkoder avklartBostedsland = Landkoder.FI;
    private Landkoder territorialfarvannLand = Landkoder.GB;

    @Before
    public void setUp() {
        søknad = new SoeknadDokument();
        Saksopplysning soeknad = new Saksopplysning();
        soeknad.setDokument(søknad);
        soeknad.setType(SaksopplysningType.SØKNAD);
        søknad.oppholdUtland.oppholdslandKoder.add(oppholdsland.getKode());
        søknad.bosted.oppgittAdresse = new StrukturertAdresse();
        søknad.bosted.oppgittAdresse.landKode = bostedsland.getKode();
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.territorialfarvann = territorialfarvannLand.getKode();
        søknad.maritimtArbeid.add(maritimtArbeid);

        when(avklartefaktaService.hentFlaggland(anyLong())).thenReturn(Optional.of(flaggland));

        lovvalgsperiode = new Lovvalgsperiode();
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Arrays.asList(lovvalgsperiode));
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(soeknad)));

        landvelgerService = new LandvelgerService(avklartefaktaService, lovvalgsperiodeService);
    }

    @Test
    public void hentArbeidsland_medArt121_girOppholdsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(oppholdsland.getBeskrivelse());
    }

    @Test
    public void hentArbeidsland_medArt121Og1141_girFlaggland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1);
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(flaggland.getBeskrivelse());
    }

    @Test
    public void hentArbeidsland_medArt122_girOppholdsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2);
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(oppholdsland.getBeskrivelse());
    }

    @Test
    public void hentArbeidsland_medArt161_girOppholdsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1);
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(oppholdsland.getBeskrivelse());
    }

    @Test
    public void hentArbeidsland_medArt113A_girTerritorialfarvannsLand() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A);
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(territorialfarvannLand.getBeskrivelse());
    }

    @Test
    public void hentArbeidsland_medArt113AOgArt1141_girFlaggLand() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1);
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(flaggland.getBeskrivelse());
    }

    @Test
    public void hentArbeidsland_medArt1142_girFlaggland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2);
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(flaggland.getBeskrivelse());
    }

    @Test
    public void hentArbeidsland_utenBestemmelseMedAvslag_girOppholdsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(oppholdsland.getBeskrivelse());
    }

    @Test(expected = FunksjonellException.class)
    public void hentArbeidsland_utenBestemmelse_girUnntak() throws FunksjonellException, TekniskException {
        String land = landvelgerService.hentArbeidsland(behandling);
        assertThat(land).isEqualTo(oppholdsland.getBeskrivelse());
    }


    @Test
    public void hentTrygdemyndighetsland_medArt121_girOppholdsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling);
        assertThat(land).isEqualTo(oppholdsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt121OgArt1141_girFlaggland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling);
        assertThat(land).isEqualTo(flaggland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt122_girOppholdsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling);
        assertThat(land).isEqualTo(oppholdsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt161_girOppholdsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling);
        assertThat(land).isEqualTo(oppholdsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt1142_girBostedsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling);
        assertThat(land).isEqualTo(bostedsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt1142OgAvklartBostedsland_girAvklartBostedsland() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_4_2);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        String land = landvelgerService.hentTrygdemyndighetsland(behandling);
        assertThat(land).isEqualTo(avklartBostedsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113A_girAvklartBosted() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling);
        assertThat(land).isEqualTo(bostedsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113AOgAvklartBosted_overstyrerOppgittBosted() throws FunksjonellException, TekniskException {
        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_3A);
        lovvalgsperiode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        String land = landvelgerService.hentTrygdemyndighetsland(behandling);
        assertThat(land).isEqualTo(avklartBostedsland.getBeskrivelse());
    }
}
