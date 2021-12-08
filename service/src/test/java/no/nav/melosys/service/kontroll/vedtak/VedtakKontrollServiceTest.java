package no.nav.melosys.service.kontroll.vedtak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.exception.validering.KontrollfeilDto;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VedtakKontrollServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private RegisteropplysningerService registeropplysningerService;

    private final long behandlingID = 1L;
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
    private final BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
    private Behandling behandling = lagBehandling(behandlingsgrunnlagData);

    private VedtakKontrollService vedtakKontrollService;

    @BeforeEach
    void setup() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());

        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);

        behandling.getSaksopplysninger().add(medlSaksopplysning);
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID)).thenReturn(lovvalgsperiode);

        final FakeUnleash unleash = new FakeUnleash();
        unleash.enable("melosys.pdl.aktiv");
        vedtakKontrollService = new VedtakKontrollService(behandlingService, behandlingsresultatService, lovvalgsperiodeService, persondataFasade,
            registeropplysningerService, unleash);
    }

    @Test
    void validerInnvilgelse_feilFraKontroller_kasterExceptionMedFeilkode() {
        var behandlingsresultat = lagBehandlingsresultat();
        when(persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentAktørID())).thenReturn("fnr");

        Consumer<ValideringException> medFeilkode = v -> assertThat(v.getFeilkoder())
            .extracting(KontrollfeilDto::getKode).containsExactly(Kontroll_begrunnelser.INGEN_SLUTTDATO.getKode());

        assertThatThrownBy(() -> vedtakKontrollService.validerInnvilgelse(behandling, behandlingsresultat, FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS))
            .isInstanceOfSatisfying(ValideringException.class, medFeilkode)
            .hasMessage("Feil i validering. Kan ikke fatte vedtak.");
    }

    @Test
    void validerInnvilgelse_oppdaterRegisteropplysninger_oppdatererRegisteropplysninger() throws ValideringException {
        lovvalgsperiode.setTom(LocalDate.now());
        var behandlingsresultat = lagBehandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        when(persondataFasade.hentFolkeregisterident(behandling.getFagsak().hentAktørID())).thenReturn("fnr");

        vedtakKontrollService.validerInnvilgelse(behandling.getId(), FØRSTEGANGSVEDTAK, true);
        verify(registeropplysningerService).hentOgLagreOpplysninger(any());
    }

    @Test
    void validerInnvilgelse_ikkeOppdaterRegisteropplysninger_oppdatererkke() throws ValideringException {
        lovvalgsperiode.setTom(LocalDate.now());

        vedtakKontrollService.validerInnvilgelse(behandling.getId(), FØRSTEGANGSVEDTAK, false);
        verify(registeropplysningerService, never()).hentOgLagreOpplysninger(any());
    }

    @Test
    void utførKontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomCollection() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS);
        assertThat(resultat).isEmpty();
    }

    @Test
    void utførKontroller_periodeOver24MndArt16IkkeOverlappendePeriode_returnererTomCollection() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS);
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

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS);
        assertThat(resultat).extracting(Kontrollfeil::getKode).containsExactlyInAnyOrder(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }

    @Test
    void utførKontroller_manglerAdresse_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    @Test
    void utførKontroller_periodeManglerSluttdato_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(null);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS);
        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.INGEN_SLUTTDATO);
    }

    @Test
    void utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsgrunnlagData.arbeidPaaLand.fysiskeArbeidssteder = List.of(new FysiskArbeidssted());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED);
    }

    @Test
    void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        behandlingsgrunnlagData.foretakUtland = List.of(new ForetakUtland());

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK, Sakstyper.EU_EOS);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ANDRE_ARBEIDSFORHOLD_UTL);
    }

    @Test
    void utførKontroller_representantIUtlandetManglerFelter_returnererKode() {
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        var behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new SoeknadTrygdeavtale());
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        Collection<Kontrollfeil> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK, Sakstyper.TRYGDEAVTALE);
        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.ANNET);
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        return behandlingsresultat;
    }
}
