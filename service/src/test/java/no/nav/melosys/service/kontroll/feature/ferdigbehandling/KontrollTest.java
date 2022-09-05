package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KontrollTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    private final long behandlingID = 1L;
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
    private final Behandling behandling = lagBehandling(behandlingsgrunnlagData);
    private Kontroll kontroll;


    @BeforeEach
    void setup() {
        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);
        behandling.getSaksopplysninger().add(medlSaksopplysning);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        lenient().when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);

        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);


        kontroll = new Kontroll(behandlingService, lovvalgsperiodeService, persondataFasade, behandlingsresultatService);
    }

    @Test
    void utførKontroller_HenleggelsePersonMedRegistrertAdresse__returnererTomCollection() {
        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.HENLEGGELSE);

        assertThat(resultat).isEmpty();
    }

    @Test
    void utførKontroller_AvslagPersonMedRegistrertAdresse__returnererTomCollection() {
        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);

        assertThat(resultat).isEmpty();
    }

    @Test
    void utførKontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomCollection() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        assertDoesNotThrow(() -> kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN));
    }

    @Test
    void utførKontroller_AvslagPersonUtenRegistrertAdresse_returnererKode() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    @Test
    void utførKontroller_HenleggelsePersonUtenRegistrertAdresse_returnererKode() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.HENLEGGELSE);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    @Test
    void utførKontroller_periodeOver24MndArt16IkkeOverlappendePeriode_returnererTomCollection() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);


        assertThat(resultat).isEmpty();
    }


    @Test
    void utførKontroller_periodeOver24MndArt12MedOverlappendePeriode_returnererCollectionMedToKoder() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2);

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusYears(2));
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode);


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);


        assertThat(resultat).extracting(Kontrollfeil::getKode).containsExactlyInAnyOrder(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }

    @Test
    void utførKontroller_periodeOver3År_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3).plusDays(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);

        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new SoeknadTrygdeavtale());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.TRYGDEAVTALE, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);


        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.MER_ENN_TRE_ÅR);
    }

    @Test
    void utførKontroller_manglerAdresse_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.IKKE_FASTSATT);


        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }


    @Test
    void utførKontroller_periodeManglerSluttdato_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(null);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);


        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.INGEN_SLUTTDATO);
    }

    @Test
    void utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsgrunnlagData.arbeidPaaLand.fysiskeArbeidssteder = List.of(new FysiskArbeidssted());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED);
    }

    @Test
    void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));

        behandlingsgrunnlagData.foretakUtland = List.of(new ForetakUtland());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL);
    }

    @Test
    void utførKontroller_representantIUtlandetMangler_returnererKode() {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);

        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new SoeknadTrygdeavtale());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.TRYGDEAVTALE, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED);
    }
}
