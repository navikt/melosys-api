package no.nav.melosys.service.kontroll.feature.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.avklartefakta.OppsummerteAvklarteFaktaService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnmodningUnntakKontrollTest {
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private OppsummerteAvklarteFaktaService oppsummerteAvklarteFaktaService;
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
        when(oppsummerteAvklarteFaktaService.hentAntallAvklarteVirksomheter(any())).thenReturn(1);

        anmodningUnntakKontrollService = new AnmodningUnntakKontrollService(
            anmodningsperiodeService, oppsummerteAvklarteFaktaService, behandlingService, persondataFasade);
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
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.arbeidPaaLand.fysiskeArbeidssteder = List.of(new FysiskArbeidssted());
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(lagBehandling(mottatteOpplysningerData));

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .containsExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED);
    }

    @Test
    void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.foretakUtland = List.of(new ForetakUtland());
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(lagBehandling(mottatteOpplysningerData));

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .containsExactly(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL);
    }

    @Test
    void utførKontroller_flereArbeidsgivere_returnererKode() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(lagBehandling());
        when(oppsummerteAvklarteFaktaService.hentAntallAvklarteVirksomheter(any())).thenReturn(2);

        Collection<Kontrollfeil> resultat = anmodningUnntakKontrollService.utførKontroller(behandlingID);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET);
    }
}
