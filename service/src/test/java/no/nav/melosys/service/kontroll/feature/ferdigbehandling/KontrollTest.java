package no.nav.melosys.service.kontroll.feature.ferdigbehandling;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Fullmaktstype;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.exception.KontrolldataFeilType;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KontrollTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;
    @Mock
    private OrganisasjonOppslagService organisasjonOppslagService;
    @Mock
    private MedlemskapsperiodeService medlemskapsperiodeService;

    private final long behandlingID = 1L;
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
    private final MottatteOpplysningerData mottatteOpplysningerData = new MottatteOpplysningerData();
    private final Behandling behandling = lagBehandling(mottatteOpplysningerData);
    private Kontroll kontroll;

    @BeforeEach
    void setup() {
        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);
        behandling.getSaksopplysninger().add(medlSaksopplysning);

        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysninger());
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);

        kontroll = new Kontroll(behandlingService, lovvalgsperiodeService, avklarteVirksomheterService, persondataFasade, organisasjonOppslagService, saksbehandlingRegler, medlemskapsperiodeService);
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
    void utførKontroller_avslagIngenFlyt__returnererTomCollection() {
        behandling.setType(Behandlingstyper.HENVENDELSE);
        behandling.setMottatteOpplysninger(null);
        when(saksbehandlingRegler.harIngenFlyt(any())).thenReturn(true);

        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);

        assertThat(resultat).isEmpty();
    }

    @Test
    void utførKontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomCollection() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);


        assertDoesNotThrow(() -> kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN));
    }

    @Test
    void utførKontroller_FTRL_returnerer_adresse_mangler() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());
        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.FTRL, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);

        assertThat(resultat).extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER);
    }

    @Test
    void utførKontroller_fullmektigFinner_adresse_mangler() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());
        var fullmektig = new Aktoer();
        fullmektig.setRolle(Aktoersroller.FULLMEKTIG);
        fullmektig.setPersonIdent("fullmektig person");
        fullmektig.setFullmaktstype(Fullmaktstype.FULLMEKTIG_SØKNAD);
        behandling.getFagsak().getAktører().add(fullmektig);

        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.FTRL, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);

        assertThat(resultat).extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_REPRESENTANT);
    }

    @Test
    void utførKontroller_AvslagPersonUtenRegistrertAdresse_returnererKode() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER);
    }

    @Test
    void utførKontroller_HenleggelsePersonUtenRegistrertAdresse_returnererKode() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.HENLEGGELSE);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER);
    }

    @Test
    void utførKontroller_HenleggelsePersonUtenRegistrertAdresse_returnererType() {
        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.HENLEGGELSE);


        assertThat(resultat)
            .extracting(Kontrollfeil::getType)
            .contains(KontrolldataFeilType.FEIL);
    }

    @Test
    void utførKontroller_periodeOver24MndArt16IkkeOverlappendePeriode_returnererTomCollection() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);


        assertThat(resultat).isEmpty();
    }


    @Test
    void utførKontroller_periodeOver24MndArt12MedOverlappendePeriode_returnererCollectionMedToKoder() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);

        Medlemsperiode medlemsperiode = new Medlemsperiode(
            null, new Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusYears(2)), null,
            PeriodestatusMedl.GYLD.getKode(), null, null, null, null, null, null);
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode);


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);


        assertThat(resultat).extracting(Kontrollfeil::getKode).containsExactlyInAnyOrder(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }

    @Test
    void utførKontroller_periodeOver3År_returnererKode() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3).plusDays(1));
        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1);

        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(new SøknadNorgeEllerUtenforEØS());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.TRYGDEAVTALE, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);


        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.MER_ENN_TRE_ÅR);
    }

    @Test
    void utførKontroller_manglerAdresse_returnererKode() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        when(persondataFasade.hentPerson(anyString())).thenReturn(PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.IKKE_FASTSATT);

        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE_BRUKER);
    }


    @Test
    void utførKontroller_periodeManglerSluttdato_returnererKode() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(null);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);


        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.INGEN_SLUTTDATO);
        assertThat(resultat)
            .extracting(Kontrollfeil::getType)
            .contains(KontrolldataFeilType.FEIL);
    }

    @Test
    void utførKontroller_periodeManglerSluttdatoOgErUnntak_returnererKode() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(null);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK);
        when(behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)).thenReturn(behandling);

        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);

        assertThat(resultat).extracting(Kontrollfeil::getKode).contains(Kontroll_begrunnelser.INGEN_SLUTTDATO);
        assertThat(resultat)
            .extracting(Kontrollfeil::getType)
            .contains(KontrolldataFeilType.FEIL);
    }

    @Test
    void utførKontroller_arbeidsstedManglerFelter_returnererKode() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        mottatteOpplysningerData.arbeidPaaLand.setFysiskeArbeidssteder(List.of(new FysiskArbeidssted()));


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSSTED_LAND);
    }

    @Test
    void utførKontroller_foretakUtlandManglerFelter_returnererKode() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));

        var foretakUtland = new ForetakUtland();
        foretakUtland.setSelvstendigNæringsvirksomhet(false);
        mottatteOpplysningerData.foretakUtland = List.of(foretakUtland);


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.MANGLENDE_OPPL_ARBEIDSFORHOLD_UTL);
    }

    @Test
    void utførKontroller_avklartVirksomhetErOpphørt_returnererKode() {
        mockReturnertLovvalgsperiode();
        when(avklarteVirksomheterService.harOpphørtAvklartVirksomhet(behandling)).thenReturn(true);

        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.EU_EOS, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);

        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.OPPHØRT_ARBEIDSGIVER);
    }

    @Test
    void utførKontroller_representantIUtlandetMangler_returnererKode() {
        mockReturnertLovvalgsperiode();

        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1);

        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(new SøknadNorgeEllerUtenforEØS());


        Collection<Kontrollfeil> resultat = kontroll.utførKontroller(behandlingID, Sakstyper.TRYGDEAVTALE, Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);


        assertThat(resultat)
            .extracting(Kontrollfeil::getKode)
            .contains(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED);
    }

    private void mockReturnertLovvalgsperiode() {
        when(lovvalgsperiodeService.hentLovvalgsperiode(behandlingID)).thenReturn(lovvalgsperiode);
    }
}
