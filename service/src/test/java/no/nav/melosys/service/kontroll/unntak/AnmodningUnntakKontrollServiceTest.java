package no.nav.melosys.service.kontroll.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.JuridiskArbeidsgiverNorge;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnmodningUnntakKontrollServiceTest {
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private PersondataFasade persondataFasade;

    private final long behandlingID = 33L;
    private final Anmodningsperiode anmodningsperiode = new Anmodningsperiode();

    private AnmodningUnntakKontrollService anmodningUnntakKontrollService;

    @BeforeEach
    void setup() {
        anmodningsperiode.setFom(LocalDate.now());
        anmodningsperiode.setTom(LocalDate.now().plusYears(2));
        when(anmodningsperiodeService.hentFørsteAnmodningsperiode(behandlingID)).thenReturn(anmodningsperiode);

        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        final FakeUnleash unleash = new FakeUnleash();
        unleash.enable("melosys.pdl.aktiv");
        anmodningUnntakKontrollService = new AnmodningUnntakKontrollService(anmodningsperiodeService, behandlingService,
            persondataFasade, unleash);
    }

    @Test
    void utførKontroller_manglerAdresse_returnererKode() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(lagBehandling());
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .containsExactly(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    @Test
    void utførKontroller_anmodningsperiodeManglerSluttdato_returnererKode() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(lagBehandling());
        anmodningsperiode.setTom(null);

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .containsExactly(Kontroll_begrunnelser.INGEN_SLUTTDATO);
    }

    @Test
    void utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.arbeidPaaLand.fysiskeArbeidssteder = List.of(new FysiskArbeidssted());
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(lagBehandling(behandlingsgrunnlagData));

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .containsExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED);
    }

    @Test
    void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.foretakUtland = List.of(new ForetakUtland());
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(lagBehandling(behandlingsgrunnlagData));

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .containsExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL);
    }

    @Test
    void utførKontroller_flereArbeidsgivereArt16_1_returnererKode() {
        anmodningsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.juridiskArbeidsgiverNorge = lagJuridiskArbeidsgiverNorge();
        behandlingsgrunnlagData.foretakUtland = lagForetakUtland();
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(lagBehandling(behandlingsgrunnlagData));

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET);
    }

    private JuridiskArbeidsgiverNorge lagJuridiskArbeidsgiverNorge() {
        var juridiskArbeidsgiverNorge = new JuridiskArbeidsgiverNorge();
        juridiskArbeidsgiverNorge.ekstraArbeidsgivere = List.of("Ekstra arbeidsgiver");
        return juridiskArbeidsgiverNorge;
    }

    private List<ForetakUtland> lagForetakUtland() {
        var foretakUtland = new ForetakUtland();
        foretakUtland.uuid = "uuid-001-123";
        return List.of(foretakUtland);
    }
}
