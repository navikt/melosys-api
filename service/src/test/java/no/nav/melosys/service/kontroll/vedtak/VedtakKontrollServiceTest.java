package no.nav.melosys.service.kontroll.vedtak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VedtakKontrollServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private PersondataFasade persondataFasade;

    private final long behandlingID = 33L;
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();

    private VedtakKontrollService vedtakKontrollService;

    @BeforeEach
    void setup() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);

        var behandling = lagBehandling(behandlingsgrunnlagData);
        behandling.getSaksopplysninger().add(medlSaksopplysning);
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID)).thenReturn(lovvalgsperiode);

        final FakeUnleash unleash = new FakeUnleash();
        unleash.enable("melosys.kontroller.pdl");
        vedtakKontrollService = new VedtakKontrollService(behandlingService, lovvalgsperiodeService, persondataFasade,
            unleash);
    }

    @Test
    void utførKontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomCollection() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).isEmpty();
    }

    @Test
    void utførKontroller_periodeOver24MndArt16IkkeOverlappendePeriode_returnererTomCollection() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).isEmpty();
    }

    @Test
    void utførKontroller_periodeOver24MndArt12MedOverlappendePeriode_returnererCollectionMedToKoder() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(2));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusYears(2));
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode);

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).extracting(Kontrollfeil::getKode).containsExactlyInAnyOrder(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }

    @Test
    void utførKontroller_manglerBostedsadresse_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenBostedsadresse());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.MANGLENDE_BOSTEDSADRESSE);
    }

    @Test
    void utførKontroller_manglerAdresse_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    @Test
    void utførKontroller_periodeManglerSluttdato_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(null);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.INGEN_SLUTTDATO);
    }

    @Test
    void utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsgrunnlagData.arbeidPaaLand.fysiskeArbeidssteder = List.of(new FysiskArbeidssted());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED);
    }

    @Test
    void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsgrunnlagData.foretakUtland = List.of(new ForetakUtland());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL);
    }
}
