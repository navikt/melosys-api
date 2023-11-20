package no.nav.melosys.service.dokument.brev.bygger;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.Bosted;
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland;
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.OrganisasjonDokumentTestFactory;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser.UTSENDELSE_OVER_24_MN;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art12_2_begrunnelser.NORMALT_IKKE_DRIFT_NORGE;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning.ERSTATTER_EN_ANNEN_UNDER_5_AAR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_uten_art12.SJOEMANNSKIRKEN;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagStrukturertAdresse;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagVilkaarsresultat;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysningerUtenBostedsadresse;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysningerUtenBostedsadresseOgKontaktadresse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrevDataByggerA001Test {
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;
    @Mock
    private UtenlandskMyndighetService myndighetsService;
    @Mock
    private VilkaarsresultatService vilkaarsresultatService;
    @Mock
    private EregFasade ereg;

    private Behandling behandling;

    private Set<String> avklarteOrganisasjoner;

    private Soeknad søknad;
    private ArbeidsforholdDokument arbDokument;

    private BrevDataByggerA001 brevDataByggerA001;

    private final String SAKSBEHANDLER_ID = "Z12345";
    private final String orgnr1 = "123456789";
    private final String orgnr2 = "987654321";

    @BeforeEach
    public void setUp() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId("ident");

        Fagsak fagsak = new Fagsak();
        fagsak.setAktører(Set.of(aktoer));

        behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);

        avklarteOrganisasjoner = new HashSet<>();
        when(avklartefaktaService.hentAvklarteOrgnrOgUuid(anyLong())).thenReturn(avklarteOrganisasjoner);

        Land_iso2 unntakFraLovvalgsland = Land_iso2.SE;
        Anmodningsperiode periode = new Anmodningsperiode();
        periode.setUnntakFraBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        periode.setUnntakFraLovvalgsland(unntakFraLovvalgsland);
        when(anmodningsperiodeService.hentAnmodningsperioder(anyLong())).thenReturn(Collections.singletonList(periode));

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        when(myndighetsService.hentUtenlandskMyndighet(any())).thenReturn(utenlandskMyndighet);

        lagVilkårResultat(Vilkaar.FO_883_2004_ART16_1, true, ERSTATTER_EN_ANNEN_UNDER_5_AAR);

        StrukturertAdresse oppgittAdresse = new StrukturertAdresse();
        oppgittAdresse.setGatenavn("HjemmeGata");
        oppgittAdresse.setHusnummerEtasjeLeilighet("23B");
        oppgittAdresse.setPostnummer("0165");
        oppgittAdresse.setPoststed("Oslo");
        oppgittAdresse.setLandkode(Landkoder.NO.getKode());

        søknad = new Soeknad();
        søknad.bosted.oppgittAdresse = oppgittAdresse;

        ForetakUtland foretakUtland = new ForetakUtland();
        foretakUtland.orgnr = orgnr1;
        søknad.foretakUtland.add(foretakUtland);

        MedlemskapDokument medlDokument = new MedlemskapDokument();
        Saksopplysning medl = new Saksopplysning();
        medl.setDokument(medlDokument);
        medl.setType(SaksopplysningType.MEDL);

        arbDokument = new ArbeidsforholdDokument();
        lagArbeidsforhold(orgnr2,
            LocalDate.of(2005, 1, 11),
            LocalDate.of(2017, 8, 11));

        Saksopplysning aareg = new Saksopplysning();
        aareg.setDokument(arbDokument);
        aareg.setType(SaksopplysningType.ARBFORH);

        behandling.setSaksopplysninger(new HashSet<>(List.of(medl, aareg)));

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(søknad);
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        OrganisasjonsDetaljer detaljer = mock(OrganisasjonsDetaljer.class);
        when(detaljer.hentStrukturertForretningsadresse()).thenReturn(lagStrukturertAdresse());

        leggTilTestorganisasjon("navn1", orgnr1, detaljer);
        leggTilTestorganisasjon("navn2", orgnr2, detaljer);

        when(vilkaarsresultatService.harVilkaarForArtikkel12(anyLong())).thenCallRealMethod();
        brevDataByggerA001 = new BrevDataByggerA001(lovvalgsperiodeService, anmodningsperiodeService, myndighetsService, vilkaarsresultatService);
    }

    private void lagVilkårResultat(Vilkaar vilkaarType, boolean oppfylt, Kodeverk begrunnelseKode) {
        Vilkaarsresultat vilkaarsresultat = lagVilkaarsresultat(vilkaarType, oppfylt, begrunnelseKode);
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(vilkaarType))).thenReturn(Optional.of(vilkaarsresultat));
    }

    private BrevDataGrunnlag lagBrevDataGrunnlag() {
        return lagBrevDataGrunnlag(PersonopplysningerObjectFactory.lagPersonopplysninger());
    }

    private BrevDataGrunnlag lagBrevDataGrunnlag(Persondata persondata) {
        return lagBrevDataGrunnlag(new DoksysBrevbestilling.Builder().medBehandling(behandling).build(), persondata);
    }

    private BrevDataGrunnlag lagBrevDataGrunnlag(DoksysBrevbestilling brevbestilling) {
        return lagBrevDataGrunnlag(brevbestilling, PersonopplysningerObjectFactory.lagPersonopplysninger());
    }

    private BrevDataGrunnlag lagBrevDataGrunnlag(DoksysBrevbestilling brevbestilling, Persondata persondata) {
        OrganisasjonOppslagService registerOppslagService = new OrganisasjonOppslagService(ereg);
        AvklarteVirksomheterService avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService, mock(BehandlingService.class), mock(KodeverkService.class));
        return new BrevDataGrunnlag(brevbestilling, mock(KodeverkService.class), avklarteVirksomheterService, avklartefaktaService, persondata);
    }

    private void leggTilTestorganisasjon(String navn, String orgnummer, OrganisasjonsDetaljer detaljer) {
        OrganisasjonDokument orgDok = OrganisasjonDokumentTestFactory.createOrganisasjonDokumentForTest();
        orgDok.setNavn(navn);
        orgDok.setOrgnummer(orgnummer);
        orgDok.setOrganisasjonDetaljer(detaljer);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.ORG);
        saksopplysning.setDokument(orgDok);
        when(ereg.hentOrganisasjon(orgnummer)).thenReturn(saksopplysning);
    }

    private Arbeidsforhold lagArbeidsforhold(String orgnr, LocalDate fom, LocalDate tom) {
        Arbeidsforhold arbeidsforhold = new Arbeidsforhold();
        arbeidsforhold.arbeidsgiverID = orgnr;
        arbeidsforhold.ansettelsesPeriode = new Periode(fom, tom);
        arbDokument.arbeidsforhold.add(arbeidsforhold);

        return arbeidsforhold;
    }

    @Test
    void testHentAvklarteSelvstendigeForetak() {
        avklarteOrganisasjoner.add(orgnr1);

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        SelvstendigForetak foretak2 = new SelvstendigForetak();
        foretak2.orgnr = orgnr2;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak2);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(lagBrevDataGrunnlag(), SAKSBEHANDLER_ID);
        assertThat(brevDataA001.selvstendigeVirksomheter.stream()
            .map(nv -> nv.orgnr)).containsOnly(foretak.orgnr);
    }

    @Test
    void testHentAvklarteNorskeForetak() {
        avklarteOrganisasjoner.add(orgnr1);

        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere.add(orgnr1);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(lagBrevDataGrunnlag(), SAKSBEHANDLER_ID);
        assertThat(brevDataA001.selvstendigeVirksomheter.stream()
            .map(nv -> nv.orgnr)).containsOnly(orgnr1);
        assertThat(brevDataA001.arbeidsgivendeVirksomheter.stream()
            .map(nv -> nv.orgnr)).containsOnly(orgnr1);
    }

    @Test
    void testIngenAvklarteforetak() {
        SelvstendigForetak foretak = new SelvstendigForetak();
        foretak.orgnr = orgnr1;
        søknad.selvstendigArbeid.selvstendigForetak.add(foretak);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(lagBrevDataGrunnlag(), SAKSBEHANDLER_ID);
        assertThat(brevDataA001.ansettelsesperiode).isEmpty();
    }

    @Test
    void lag_art16MedArt121_harKunArt16Begrunnelser() {
        lagVilkårResultat(Vilkaar.FO_883_2004_ART12_1, false, UTSENDELSE_OVER_24_MN);
        lagVilkårResultat(Vilkaar.FO_883_2004_ART16_1, true, ERSTATTER_EN_ANNEN_UNDER_5_AAR);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(lagBrevDataGrunnlag(), SAKSBEHANDLER_ID);
        assertThat(brevDataA001.anmodningUtenArt12Begrunnelser).isEmpty();

        assertThat(brevDataA001.anmodningBegrunnelser).hasSize(1);
        assertThat(brevDataA001.anmodningBegrunnelser.stream().map(VilkaarBegrunnelse::getKode))
            .containsExactly(ERSTATTER_EN_ANNEN_UNDER_5_AAR.getKode());
    }

    @Test
    void lag_art16MedArt122_harKunArt16Begrunnelser() {
        lagVilkårResultat(Vilkaar.FO_883_2004_ART12_2, false, NORMALT_IKKE_DRIFT_NORGE);
        lagVilkårResultat(Vilkaar.FO_883_2004_ART16_1, true, ERSTATTER_EN_ANNEN_UNDER_5_AAR);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(lagBrevDataGrunnlag(), SAKSBEHANDLER_ID);
        assertThat(brevDataA001.anmodningUtenArt12Begrunnelser).isEmpty();

        assertThat(brevDataA001.anmodningBegrunnelser).hasSize(1);
        assertThat(brevDataA001.anmodningBegrunnelser.stream().map(VilkaarBegrunnelse::getKode))
            .containsExactly(ERSTATTER_EN_ANNEN_UNDER_5_AAR.getKode());
    }

    @Test
    void lag_art16UtenArt12_harKunArt16UtenArt12Begrunnelser() {
        lagVilkårResultat(Vilkaar.FO_883_2004_ART16_1, true, SJOEMANNSKIRKEN);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(lagBrevDataGrunnlag(), SAKSBEHANDLER_ID);
        assertThat(brevDataA001.anmodningBegrunnelser).isEmpty();

        assertThat(brevDataA001.anmodningUtenArt12Begrunnelser).hasSize(1);
        assertThat(brevDataA001.anmodningUtenArt12Begrunnelser.stream().map(VilkaarBegrunnelse::getKode))
            .containsExactly(SJOEMANNSKIRKEN.getKode());
    }


    @Test
    void testAnsettelsesperiode() {
        avklarteOrganisasjoner.add(orgnr1);

        lagArbeidsforhold(orgnr1,
            LocalDate.of(1976, 10, 23),
            LocalDate.of(1978, 10, 23));

        Arbeidsforhold forventet = lagArbeidsforhold(orgnr1,
            LocalDate.of(2005, 1, 11),
            LocalDate.of(2018, 8, 11));

        // Senere arbeidsforhold, men ikke et valgt arbeidsforhold
        lagArbeidsforhold(orgnr2,
            LocalDate.of(2010, 10, 23),
            LocalDate.of(2017, 10, 23));

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(lagBrevDataGrunnlag(), SAKSBEHANDLER_ID);
        assertThat((brevDataA001).ansettelsesperiode).contains(forventet.ansettelsesPeriode);
    }

    @Test
    void testIngenAnsettelsePeriode() {
        avklarteOrganisasjoner.add(orgnr1);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(lagBrevDataGrunnlag(), SAKSBEHANDLER_ID);
        assertThat(brevDataA001.ansettelsesperiode).isNotPresent();
    }

    @Test
    void lagBrevdata_ytterligereInfoFraBestilling_infoFinnes() {
        final String forventetInfo = "By the way...";
        DoksysBrevbestilling brevbestilling = new DoksysBrevbestilling.Builder()
            .medBehandling(behandling)
            .medYtterligereInformasjon(forventetInfo).build();
        BrevDataGrunnlag brevdataGrunnlag = lagBrevDataGrunnlag(brevbestilling);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(brevdataGrunnlag, SAKSBEHANDLER_ID);
        assertThat(brevDataA001.ytterligereInformasjon).isEqualTo(forventetInfo);
    }

    @Test
    void lagBrevdata_harIkkeBostedsadresse_brukerKontaktadresse() {
        søknad.bosted = new Bosted();
        var doksysBrevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        var personopplysninger = lagPersonopplysningerUtenBostedsadresse();
        BrevDataGrunnlag brevdataGrunnlag = lagBrevDataGrunnlag(doksysBrevbestilling, personopplysninger);

        BrevDataA001 brevDataA001 = (BrevDataA001) brevDataByggerA001.lag(brevdataGrunnlag, SAKSBEHANDLER_ID);
        assertThat(brevDataA001.bostedsadresse).isEqualTo(personopplysninger.finnKontaktadresse().get().hentEllerLagStrukturertAdresse());
    }

    @Test
    void lagBrevdata_harIkkeBostedsadresseEllerKontaktadresse_kasterFeilmelding() {
        søknad.bosted = new Bosted();
        var doksysBrevbestilling = new DoksysBrevbestilling.Builder().medBehandling(behandling).build();
        var personopplysninger = lagPersonopplysningerUtenBostedsadresseOgKontaktadresse();
        BrevDataGrunnlag brevdataGrunnlag = lagBrevDataGrunnlag(doksysBrevbestilling, personopplysninger);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> brevDataByggerA001.lag(brevdataGrunnlag, SAKSBEHANDLER_ID))
            .withMessageContaining("Finner verken bostedsadresse eller kontaktadresse");
    }
}
