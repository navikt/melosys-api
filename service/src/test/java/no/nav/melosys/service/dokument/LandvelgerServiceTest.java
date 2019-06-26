package no.nav.melosys.service.dokument;

import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
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
    Behandling behandling;

    private SoeknadDokument søknad;
    private LandvelgerService landvelgerService;
    private List<Vilkaarsresultat> vilkaar = new ArrayList<>();

    private Landkoder søknadsland = Landkoder.NO;
    private Landkoder avklartArbeidsland = Landkoder.DK;
    private Landkoder oppgittbostedsland = Landkoder.SE;
    private Landkoder avklartBostedsland = Landkoder.FI;
    private Landkoder territorialfarvannLand = Landkoder.GB;

    @Before
    public void setUp() {
        søknad = new SoeknadDokument();
        Saksopplysning soeknad = new Saksopplysning();
        soeknad.setDokument(søknad);
        soeknad.setType(SaksopplysningType.SØKNAD);
        søknad.oppholdUtland.oppholdslandkoder.add("NO");
        søknad.soeknadsland.landkoder.add(søknadsland.getKode());
        søknad.bosted.oppgittAdresse.landkode = oppgittbostedsland.getKode();
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.territorialfarvann = territorialfarvannLand.getKode();
        søknad.maritimtArbeid.add(maritimtArbeid);

        when(vilkaarsresultatRepository.findByBehandlingsresultatId(anyLong())).thenReturn(vilkaar);
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(soeknad)));

        landvelgerService = new LandvelgerService(avklartefaktaService, vilkaarsresultatRepository);
    }

    private void oppfyll(Vilkaar vilkaarType) {
        Vilkaarsresultat resultat = new Vilkaarsresultat();
        resultat.setVilkaar(vilkaarType);
        resultat.setOppfylt(true);
        vilkaar.add(resultat);
    }

    @Test
    public void hentArbeidsland_utenAvklartArbeidsland_girSøknadsland() throws TekniskException {
        String land = landvelgerService.hentArbeidsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(søknadsland.getBeskrivelse());
    }

    @Test
    public void hentArbeidsland_medAvklartArbeidsland_girAvklartArbeidsland() throws TekniskException {
        when(avklartefaktaService.hentArbeidsland(anyLong())).thenReturn(Optional.of(avklartArbeidsland));

        String land = landvelgerService.hentArbeidsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(avklartArbeidsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt121_girSøknadsland() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART12_1);
        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(søknadsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt121AvklartArbeidsland_girAvklartArbeidsland() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART12_1);
        when(avklartefaktaService.hentArbeidsland(anyLong())).thenReturn(Optional.of(avklartArbeidsland));
        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(avklartArbeidsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt122_girSøknadsland() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART12_2);
        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(søknadsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt161_girSøknadsland() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART16_1);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(søknadsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt1142_girOppgittBostedsland() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART11_4_2);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(oppgittbostedsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt1142OgAvklartBostedsland_girAvklartBostedsland() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART11_4_2);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(avklartBostedsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113A_girOppgittBostedsland() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);

        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(oppgittbostedsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113AOgAvklartBosted_overstyrerOppgittBosted() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);
        when(avklartefaktaService.hentBostedland(anyLong())).thenReturn(Optional.of(avklartBostedsland));

        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(avklartBostedsland.getBeskrivelse());
    }

    @Test
    public void hentTrygdemyndighetsland_medArt113AUtenOppgittEllerAvkartBostedsland_girNorge() throws TekniskException {
        oppfyll(Vilkaar.FO_883_2004_ART11_3A);
        oppfyll(Vilkaar.FO_883_2004_ART11_4_1);

        søknad.bosted.oppgittAdresse.landkode = null;

        String land = landvelgerService.hentTrygdemyndighetsland(behandling).getBeskrivelse();
        assertThat(land).isEqualTo(Landkoder.NO.getBeskrivelse());
    }
}