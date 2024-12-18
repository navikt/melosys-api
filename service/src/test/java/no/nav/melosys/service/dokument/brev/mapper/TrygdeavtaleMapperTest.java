package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseOgAttestTrygdeavtale;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.attest.AttestTrygdeavtale;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse.Barn;
import no.nav.melosys.integrasjon.dokgen.dto.trygdeavtale.innvilgelse.InnvilgelseTrygdeavtale;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.TRYGDEAVTALE_GB;
import static no.nav.melosys.service.dokument.DokgenTestData.*;
import static no.nav.melosys.service.dokument.brev.mapper.DokgenMalMapperTest.SOKNADSDATO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrygdeavtaleMapperTest {
    private static final String UUID_EKTEFELLE = "uuidEktefelle";
    private static final String UUID_BARN_1 = "uuidBarn1";
    private static final String UUID_BARN_2 = "uuidBarn2";
    private static final String UUID_BARN_3 = "uuidBarn3";
    private static final String EKTEFELLE_FNR = "01108049800";
    private static final String BARN1_FNR = "01100099728";
    private static final String BARN2_FNR = "02109049878";
    private static final String BARN_NAVN_1 = "Doffen Duck";
    private static final String BARN_NAVN_2 = "Dole Duck";
    private static final String BARN_NAVN_3 = "Utenid Duck";
    private static final String BARN3_UTEN_FNR = "01.02.2021";
    private static final String ARBEIDSGIVER_NAVN = "Bang Hansen";
    private static final String EKTEFELLE_NAVN = "Dolly Duck";
    private static final String ORG_NR = "987654321";
    private static final LocalDate VEDTAKS_DATO = LocalDate.of(2022, 1, 1);
    private static final Instant VEDTAKS_DATO_INSTANT = VEDTAKS_DATO.atStartOfDay(ZoneId.systemDefault()).toInstant();

    @Mock
    private AvklarteMedfolgendeFamilieService mockAvklarteMedfolgendeFamilieService;
    @Mock
    private LovvalgsperiodeService mockLovvalgsperiodeService;
    @Mock
    private AvklarteVirksomheterService mockAvklarteVirksomheterService;
    @Mock
    private UtledMottaksdato utledMottaksdato;

    private final FakeUnleash unleash = new FakeUnleash();

    private TrygdeavtaleMapper trygdeavtaleMapper;

    @BeforeEach
    void setup() {
        unleash.enableAll();
        trygdeavtaleMapper = new TrygdeavtaleMapper(
            mockAvklarteMedfolgendeFamilieService,
            mockAvklarteVirksomheterService,
            mockLovvalgsperiodeService,
            utledMottaksdato,
            unleash);
    }

    @Test
    void map_altOkInnvilgelseOgAttestSendes_populererFelter() throws JsonProcessingException {
        mockMedfølgendeFamilieDefaultCase();
        mockAvklartFamilieDefaultCase();
        mockHappyCase();

        InnvilgelseBrevbestilling brevbestilling = lagStorbritanniaBrevbestilling(medPeriode(lagTrygdeavtaleBehandling()));

        InnvilgelseOgAttestTrygdeavtale innvilgelseOgAttestTrygdeavtale = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB);

        String jsonInnvilgelse = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(innvilgelseOgAttestTrygdeavtale.getInnvilgelse());
        String resultatInnvilgelse = jsonInnvilgelse.replaceAll("(\"dagensDato\" :)(.*)", "$1 \"Fjernet for test\",");

        assertThat(innvilgelseOgAttestTrygdeavtale.isSkalHaAttest()).isTrue();
        assertThat(resultatInnvilgelse).isEqualToIgnoringNewLines(FORVENTEDE_FELTER_FOR_INNVILGELSE_STORBRITANNIA_MAPPING);

        String jsonAttest = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(innvilgelseOgAttestTrygdeavtale.getAttest());
        String resultatAttest = jsonAttest.replaceAll("(\"dagensDato\" :)(.*)", "$1 \"Fjernet for test\",");

        assertThat(innvilgelseOgAttestTrygdeavtale.isSkalHaInnvilgelse()).isTrue();
        assertThat(resultatAttest).isEqualToIgnoringNewLines(FORVENTEDE_FELTER_FOR_ATTEST_STORBRITANNIA_MAPPING);

        assertThat(innvilgelseOgAttestTrygdeavtale.isSkalHaInfoOmRettigheter()).isTrue();
        assertThat(innvilgelseOgAttestTrygdeavtale.getNyVurderingBakgrunn()).isEqualTo(brevbestilling.getNyVurderingBakgrunn());
    }

    @Test
    void map_utenlandske_virksomheter_populererFelter() {
        mockHappyCase();

        when(mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any())).thenReturn(Collections.emptyList());
        when(mockAvklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(lagAvklarteVirksomheter());

        var brevbestilling = lagStorbritanniaBrevbestilling(lagTrygdeavtaleBehandling());

        var result = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB);

        assertNotNull(result, "Resultat skal ikke være null");
    }

    private static List<Arguments> sjekkAdresser() {
        return TrygdeavtaleAdresseSjekkerTest.sjekkAdresser();
    }

    @ParameterizedTest(name = "{6}")
    @MethodSource("sjekkAdresser")
    void map_brukNorskBostedsAddresse(
        Land_iso2 landkodeBosted,
        Land_iso2 landkodeOpphold,
        Land_iso2 landkodeKontakt,
        List<String> norskAddresse,
        List<String> ukAddresse,
        Land_iso2 soknadsland,
        String grunn) {

        mockHappyCase();
        var persondata = TrygdeavtaleAdresseSjekkerTest.lagPersonopplysninger(landkodeBosted, landkodeOpphold, landkodeKontakt, Optional.empty(), Optional.empty(), Optional.empty());
        InnvilgelseBrevbestilling brevbestilling =
            lagStorbritanniaBrevbestillingDefaultBuilder(medPeriode(lagTrygdeavtaleBehandling()))
                .medPersonDokument(persondata)
                .build();

        InnvilgelseOgAttestTrygdeavtale innvilgelseOgAttestTrygdeavtale = trygdeavtaleMapper.map(brevbestilling, soknadsland);
        assertTrue(innvilgelseOgAttestTrygdeavtale.isSkalHaAttest());

        AttestTrygdeavtale attest = innvilgelseOgAttestTrygdeavtale.getAttest();
        List<String> bostedsadresse = attest.getArbeidstaker().bostedsadresse();
        List<String> oppholdsadresse = attest.getUtsendelse().oppholdsadresse();

        assertThat(bostedsadresse).isEqualTo(norskAddresse);
        assertThat(oppholdsadresse).isEqualTo(ukAddresse);
    }

    @Test
    void map_ingenRepresentantIUtlandet_kastFunksjonellException() {
        mockHappyCase();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> trygdeavtaleMapper.map(lagStorbritanniaBrevbestilling(lagTrygdeavtaleBehandling(null)), Land_iso2.GB))
            .withMessageContaining(Kontroll_begrunnelser.ATTEST_MANGLER_ARBEIDSSTED.getBeskrivelse());
    }

    @Test
    void map_ingenVirksomheter_kastFunksjonellException() {
        mockLovvalgsperiode();

        when(mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any())).thenReturn(Collections.emptyList());
        when(mockAvklarteVirksomheterService.hentUtenlandskeVirksomheter(any())).thenReturn(Collections.emptyList());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> trygdeavtaleMapper.map(lagStorbritanniaBrevbestilling(lagTrygdeavtaleBehandling()), Land_iso2.GB))
            .withMessageContaining("Fant 0 avklarte virksomheter for behandling: ")
            .withMessageContaining("Må være 1 for trygdeavtale");
    }

    @Test
    void map_ettOmfattetBarn_minstEttOmfattetFamiliemedlemErTrue() {
        mockLovvalgsperiode();
        mockMedfølgendeFamilieDefaultCase();
        mockHappyCase();
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagOmfattetMedfølgendeBarn());

        InnvilgelseBrevbestilling brevbestilling = lagStorbritanniaBrevbestilling(lagTrygdeavtaleBehandling());
        InnvilgelseTrygdeavtale map = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB).getInnvilgelse();
        assertThat(map.getFamilie().minstEttOmfattetFamiliemedlem()).isTrue();
    }

    @Test
    void map_barnUtenFnr_parseOppgittFnrTilDato() {
        mockMedfølgendeFamilieDefaultCase();
        mockHappyCase();
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(tomFamilie());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedølgendeBarnUtenFnr());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagBarnUtenFnr());

        InnvilgelseBrevbestilling brevbestilling = lagStorbritanniaBrevbestilling(lagTrygdeavtaleBehandling());
        InnvilgelseTrygdeavtale map = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB).getInnvilgelse();
        assertThat(map.getFamilie().barn())
            .hasSize(1)
            .element(0)
            .extracting(Barn::fnr, Barn::foedselsdato)
            .containsExactly(null, LocalDate.of(2021, 2, 1));
    }

    @Test
    void map_ingenOmfattet_minstEttOmfattetFamiliemedlemErFalse() {
        mockMedfølgendeFamilieDefaultCase();
        mockLovvalgsperiode();
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(List.of(lagLovvalgsperiode()));
        when(mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any())).thenReturn(lagAvklarteVirksomheter());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeBarn());

        InnvilgelseBrevbestilling brevbestilling = lagStorbritanniaBrevbestilling(lagTrygdeavtaleBehandling());
        InnvilgelseTrygdeavtale map = trygdeavtaleMapper.map(brevbestilling, Land_iso2.GB).getInnvilgelse();
        assertThat(map.getFamilie().minstEttOmfattetFamiliemedlem()).isFalse();
    }

    @Test
    void map_medToLovvalgperioder_kastFunksjonellException() {
        mockHappyCase();
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong()))
            .thenReturn(List.of(lagLovvalgsperiode(), lagLovvalgsperiode()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() ->
                trygdeavtaleMapper.map(lagStorbritanniaBrevbestilling(lagTrygdeavtaleBehandling()), Land_iso2.GB)
            ).withMessageContaining("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant 2");
    }

    @Test
    void map_medIngenLovvalgperioder_kastFunksjonellException() {
        mockHappyCase();
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong()))
            .thenReturn(List.of());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() ->
                trygdeavtaleMapper.map(new InnvilgelseBrevbestilling.Builder()
                    .medBehandling(lagTrygdeavtaleBehandling())
                    .medPersonDokument(lagPersondata())
                    .medVedtaksdato(VEDTAKS_DATO_INSTANT)
                    .build(), Land_iso2.GB
                )
            ).withMessageContaining("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant 0");
    }

    private void mockMedfølgendeFamilieDefaultCase() {
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(anyLong())).thenReturn(lagMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedfølgendeBarn());
    }

    private void mockAvklartFamilieDefaultCase() {
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagAvklartMedfølgendeBarn());
    }

    private void mockLovvalgsperiode() {
        when(mockLovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lagLovvalgsperiode());
    }

    private Behandling medPeriode(Behandling behandling) {
        when(utledMottaksdato.getMottaksdato(behandling)).thenReturn(SOKNADSDATO);
        return behandling;
    }

    private InnvilgelseBrevbestilling.Builder lagStorbritanniaBrevbestillingDefaultBuilder(Behandling behandling) {
        return new InnvilgelseBrevbestilling.Builder()
            .medProduserbartdokument(TRYGDEAVTALE_GB)
            .medPersonDokument(lagPersondata())
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("innledningFritekst")
            .medBegrunnelseFritekst("begrunnelse")
            .medBarnFritekst("barnFritekst")
            .medEktefelleFritekst("ektefelleFritekst")
            .medVedtaksdato(VEDTAKS_DATO_INSTANT)
            .medVirksomhetArbeidsgiverSkalHaKopi(false);
    }


    private InnvilgelseBrevbestilling lagStorbritanniaBrevbestilling(Behandling behandling) {
        return lagStorbritanniaBrevbestillingDefaultBuilder(behandling).build();
    }

    private AvklarteMedfolgendeFamilie lagOmfattetMedfølgendeEktefelle() {
        var ektefelle = new OmfattetFamilie(UUID_EKTEFELLE);
        return new AvklarteMedfolgendeFamilie(Set.of(ektefelle), Set.of());
    }

    private AvklarteMedfolgendeFamilie lagIkkeOmfattetMedfølgendeEktefelle() {
        var ektefelle = new IkkeOmfattetFamilie(UUID_EKTEFELLE,
            Medfolgende_ektefelle_samboer_begrunnelser_ftrl.MANGLER_OPPLYSNINGER.getKode(), "");
        return new AvklarteMedfolgendeFamilie(Set.of(), Set.of(ektefelle));
    }

    private AvklarteMedfolgendeFamilie lagOmfattetMedfølgendeBarn() {
        var barn = new OmfattetFamilie(UUID_BARN_1);
        barn.setSammensattNavn(BARN_NAVN_1);
        barn.setIdent(BARN1_FNR);
        return new AvklarteMedfolgendeFamilie(
            Set.of(barn),
            Set.of());
    }

    private AvklarteMedfolgendeFamilie lagIkkeOmfattetMedfølgendeBarn() {
        var barn = new IkkeOmfattetFamilie(
            UUID_BARN_1,
            Medfolgende_barn_begrunnelser.MANGLER_OPPLYSNINGER.getKode(), "");
        return new AvklarteMedfolgendeFamilie(
            Set.of(),
            Set.of(barn));
    }

    private Map<String, MedfolgendeFamilie> lagMedølgendeBarnUtenFnr() {
        return Map.of(
            UUID_BARN_3,
            MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN_3, BARN3_UTEN_FNR, BARN_NAVN_3, MedfolgendeFamilie.Relasjonsrolle.BARN)
        );
    }

    private AvklarteMedfolgendeFamilie lagBarnUtenFnr() {
        var barn = new OmfattetFamilie(UUID_BARN_3);
        barn.setSammensattNavn(BARN_NAVN_3);
        barn.setIdent(BARN3_UTEN_FNR);
        return new AvklarteMedfolgendeFamilie(Set.of(barn), Set.of());
    }

    private AvklarteMedfolgendeFamilie tomFamilie() {
        return new AvklarteMedfolgendeFamilie(Set.of(), Set.of());
    }

    private void mockHappyCase() {
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagAvklartMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagAvklartMedfølgendeBarn());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(anyLong())).thenReturn(lagMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedfølgendeBarn());
        when(mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any())).thenReturn(lagAvklarteVirksomheter());
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(List.of(lagLovvalgsperiode()));
        when(mockLovvalgsperiodeService.hentLovvalgsperiode(anyLong())).thenReturn(lagLovvalgsperiode());
    }

    static Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LOVVALGSPERIODE_FOM);
        lovvalgsperiode.setTom(LOVVALGSPERIODE_TOM);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1);
        return lovvalgsperiode;
    }

    private List<AvklartVirksomhet> lagAvklarteVirksomheter() {
        return List.of(new AvklartVirksomhet(ARBEIDSGIVER_NAVN, ORG_NR, BrevDataTestUtils.lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID));
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeEktefelle() {
        OmfattetFamilie ektefelle = new OmfattetFamilie(UUID_EKTEFELLE);
        return new AvklarteMedfolgendeFamilie(Set.of(ektefelle), Set.of());
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeBarn() {
        var b1 = new OmfattetFamilie(UUID_BARN_1);
        b1.setIdent(BARN1_FNR);
        var b2 = new IkkeOmfattetFamilie(
            UUID_BARN_2,
            Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
            null);
        b2.setIdent(BARN2_FNR);
        return new AvklarteMedfolgendeFamilie(
            Set.of(b1),
            Set.of(b2));
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeEktefelle() {
        MedfolgendeFamilie ektefelle = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER);
        return Map.of(UUID_EKTEFELLE, ektefelle);
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeBarn() {
        return Map.of(
            UUID_BARN_1,
            MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN_1, BARN1_FNR, BARN_NAVN_1, MedfolgendeFamilie.Relasjonsrolle.BARN),
            UUID_BARN_2,
            MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN_2, BARN2_FNR, BARN_NAVN_2, MedfolgendeFamilie.Relasjonsrolle.BARN)
        );
    }


    private static final String FORVENTEDE_FELTER_FOR_INNVILGELSE_STORBRITANNIA_MAPPING = String.format("""
            {
              "innvilgelse" : {
                "innledningFritekst" : "innledningFritekst",
                "begrunnelseFritekst" : "begrunnelse",
                "ektefelleFritekst" : "ektefelleFritekst",
                "barnFritekst" : "barnFritekst"
              },
              "artikkel" : "UK_ART6_1",
              "soknad" : {
                "soknadsdato" : "%s",
                "periodeFom" : "%s",
                "periodeTom" : "%s",
                "virksomhetsnavn" : "Bang Hansen",
                "soknadsland" : "Storbritannia"
              },
              "familie" : {
                "minstEttOmfattetFamiliemedlem" : true,
                "ektefelle" : {
                  "navn" : "Dolly Duck",
                  "omfattet" : true,
                  "begrunnelse" : null,
                  "fnr" : "%s",
                  "dnr" : null,
                  "foedselsdato" : "%s"
                },
                "barn" : [ {
                  "navn" : "Doffen Duck",
                  "omfattet" : true,
                  "begrunnelse" : null,
                  "fnr" : "%s",
                  "dnr" : null,
                  "foedselsdato" : "%s"
                }, {
                  "navn" : "Dole Duck",
                  "omfattet" : false,
                  "begrunnelse" : "OVER_18_AR",
                  "fnr" : "%s",
                  "dnr" : null,
                  "foedselsdato" : "%s"
                } ]
              },
              "virksomhetArbeidsgiverSkalHaKopi" : false
            }""",
        SOKNADSDATO,
        LOVVALGSPERIODE_FOM,
        LOVVALGSPERIODE_TOM,
        EKTEFELLE_FNR,
        LocalDate.of(1980, 10, 1),
        BARN1_FNR,
        LocalDate.of(2000, 10, 1),
        BARN2_FNR,
        LocalDate.of(1990, 10, 2)
    );

    private static final String FORVENTEDE_FELTER_FOR_ATTEST_STORBRITANNIA_MAPPING = String.format("""
            {
              "arbeidstaker" : {
                "navn" : "Donald Duck",
                "foedselsdato" : null,
                "fnr" : "05058892382",
                "bostedsadresse" : [ "Andebygata 1 42 C", "9999", "Norge" ]
              },
              "medfolgendeFamiliemedlemmer" : {
                "ektefelle" : {
                  "navn" : "Dolly Duck",
                  "foedselsdato" : "%s",
                  "fnr" : "%s",
                  "dnr" : null
                },
                "barn" : [ {
                  "navn" : "Doffen Duck",
                  "foedselsdato" : "%s",
                  "fnr" : "%s",
                  "dnr" : null
                } ]
              },
              "arbeidsgiverNorge" : {
                "virksomhetsnavn" : "Bang Hansen",
                "fullstendigAdresse" : [ "Strukturert Gate 12B", "4321", "Poststed", "Bulgaria" ]
              },
              "utsendelse" : {
                "artikkel" : "UK_ART6_1",
                "oppholdsadresse" : [ "Unknown" ],
                "startdato" : "%s",
                "sluttdato" : "%s"
              },
              "representant" : {
                "navn" : "Foretaksnavn",
                "adresse" : [ "Uk address" ]
              },
              "vedtaksdato" : "%s"
            }""",
        LocalDate.of(1980, 10, 1),
        EKTEFELLE_FNR,
        LocalDate.of(2000, 10, 1),
        BARN1_FNR,
        LOVVALGSPERIODE_FOM,
        LOVVALGSPERIODE_TOM,
        VEDTAKS_DATO
    );
}
