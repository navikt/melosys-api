package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.domain.person.adresse.PersonAdresse;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia.AttestStorbritannia;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static no.nav.melosys.service.dokument.DokgenTestData.lagBehandling;
import static no.nav.melosys.service.dokument.DokgenTestData.lagPersonDokument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrygdeavtaleAttestMapperTest {
    public static final String UUID_EKTEFELLE = "uuidEktefelle";
    public static final String UUID_BARN_1 = "uuidBarn1";
    public static final String EKTEFELLE_FNR = "09080723451";
    private static final String BARN1_FNR = "12131456789";
    public static final String ARBEIDSGIVER_NAVN = "Bang Hansen";
    public static final String SAKSNUMMER = "MEL-123";
    public static final String EKTEFELLE_NAVN = "Dolly Duck";
    public static final String BARN1_NAVN = "Doffen Duck";
    public static final String ORG_NR = "987654321";

    @Mock
    private AvklarteVirksomheterService mockAvklarteVirksomheterService;

    @Mock
    private AvklarteMedfolgendeFamilieService mockAvklarteMedfolgendeFamilieService;

    @Mock
    private DokgenMapperDatahenter mockDokgenMapperDatahenter;

    @Mock
    PersondataFasade mockPersondataFasade;

    @Mock
    private LovvalgsperiodeService mockLovvalgsperiodeService;

    @Mock
    Persondata mockPersondata;

    TrygdeavtaleAttestMapper trygdeavtaleAttestMapper;

    @BeforeEach
    void setup() {
        trygdeavtaleAttestMapper = new TrygdeavtaleAttestMapper(
            mockAvklarteMedfolgendeFamilieService,
            mockAvklarteVirksomheterService,
            mockDokgenMapperDatahenter,
            mockPersondataFasade,
            mockLovvalgsperiodeService);
    }

    private static List<Arguments> gyldigePerioder() {
        return List.of(
            Arguments.of(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1),

                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1),
                "lovalgsperiode er lik adresseperiode"
            ),
            Arguments.of(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1),

                LocalDate.of(2020, 2, 1),
                LocalDate.of(2020, 3, 1),
                "lovalgsperiode har start før og slutt etter adresseperiode"
            ),
            Arguments.of(
                LocalDate.of(2020, 2, 1),
                LocalDate.of(2020, 3, 1),

                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1),
                "adresseperiode har start før og slutt etter lovalgsperiode"
            ),
            Arguments.of(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2022, 1, 1),

                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1),
                "lovalgsperiode start er lik adresseperiode slutt"
            ),
            Arguments.of(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1),

                LocalDate.of(2021, 1, 1),
                LocalDate.of(2022, 1, 1),
                "lovalgsperiode slutt er lik adresseperiode start"
            )
        );
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("gyldigePerioder")
    void sjekkOmAdresseGyldighetErInnenforLovalgsperiode_for_gyldigePerioder(LocalDate lovFom, LocalDate lovTom, LocalDate gyldigFom, LocalDate gyldigTom, String grunn) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(lovFom);
        lovvalgsperiode.setTom(lovTom);
        PersonAdresse personAdresse = new Oppholdsadresse(null,
            null,
            gyldigFom,
            gyldigTom,
            null,
            null,
            null,
            false
        );
        assertThat(TrygdeavtaleAttestMapper.sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .withFailMessage(grunn)
            .isTrue();
    }

    private static List<Arguments> ugyldigePerioder() {
        return List.of(
            Arguments.of(
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2020, 1, 1),

                LocalDate.of(2020, 2, 1),
                LocalDate.of(2021, 1, 1),
                "lovalgsperiode er før adresseperiode"
            ),
            Arguments.of(
                LocalDate.of(2021, 1, 2),
                LocalDate.of(2022, 1, 1),

                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1),
                "lovalgsperiode er etter adresseperiode"
            ),
            Arguments.of(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2021, 1, 1),

                null,
                null,
                "adresseperiode fom og tom er null"
            )
        );
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("ugyldigePerioder")
    void sjekkOmAdresseGyldighetErInnenforLovalgsperiode_for_ugyldigePerioder(LocalDate lovFom, LocalDate lovTom, LocalDate gyldigFom, LocalDate gyldigTom, String grunn) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(lovFom);
        lovvalgsperiode.setTom(lovTom);
        PersonAdresse personAdresse = new Oppholdsadresse(null,
            null,
            gyldigFom,
            gyldigTom,
            null,
            null,
            null,
            false
        );
        assertThat(TrygdeavtaleAttestMapper.sjekkOmAdresseGyldighetErInnenforLovalgsperiode(personAdresse, lovvalgsperiode))
            .withFailMessage(grunn)
            .isFalse();
    }

    @Test
    void map_InnvilgetMedOmfattetFamilie_populererFelter() throws JsonProcessingException {
        mockHappyCase();

        AttestStorbritannia attestStorbritannia = trygdeavtaleAttestMapper.map(new DokgenBrevbestilling.Builder()
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersonDokument())
            .medVedtaksdato(Instant.parse("1970-10-10T00:00:00Z"))
            .build()
        );

        String forvented = """
        {
          "saksopplysninger" : {
            "saksnummer" : "MEL-123",
            "navnBruker" : "Donald Duck",
            "fnr" : "05058892382"
          },
          "mottaker" : {
            "navn" : "Donald Duck",
            "adresselinjer" : [ "Andebygata 1", null, null, null ],
            "postnr" : "9999",
            "poststed" : "Andeby",
            "land" : null,
            "type" : "BRUKER"
          },
          "arbeidstaker" : {
            "navn" : "Donald Duck",
            "foedselsdato" : null,
            "fnr" : "05058892382",
            "bostedadresse" : [ "Andebygata 1", null, null, null ]
          },
          "medfolgendeFamiliemedlemmer" : {
            "ektefelle" : {
              "navn" : "Dolly Duck",
              "foedselsdato" : "1969-12-31T23:00:00Z",
              "fnr" : "09080723451",
              "dnr" : null
            },
            "barn" : [ {
              "navn" : "Doffen Duck",
              "foedselsdato" : "1969-12-31T23:00:00Z",
              "fnr" : "12131456789",
              "dnr" : null
            } ]
          },
          "arbeidsgiverNorge" : {
            "virksomhetsnavn" : "Bang Hansen",
            "fullstendigAdresse" : [ "Strukturert Gate 12B", "4321", "Poststed", "Bulgaria" ]
          },
          "utsendelse" : {
            "artikkel" : "UK_ART6_1",
            "oppholdsadresseUK" : [ ],
            "startdato" : "2019-12-31T23:00:00Z",
            "sluttdato" : "2020-12-31T23:00:00Z"
          },
          "representantUK" : {
            "navn" : "Mrs. London",
            "adresse" : [ ]
          },
          "vedtaksdato" : "1970-10-10T00:00:00Z"
        }""";

        String s = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(attestStorbritannia);
        String resultat = s.replaceAll("\"dagensDato\" :.*\n", "");

        assertThat(resultat).isEqualToIgnoringWhitespace(forvented);
    }

    @Test
    void map_medToLovvalgperioder_kastFunksjonellException() {
        mockHappyCase();
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong()))
            .thenReturn(List.of(lagLovvalgsperiode(), lagLovvalgsperiode()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() ->
                trygdeavtaleAttestMapper.map(new DokgenBrevbestilling.Builder()
                    .medBehandling(lagBehandling())
                    .medPersonDokument(lagPersonDokument())
                    .medVedtaksdato(Instant.parse("1970-10-10T00:00:00Z"))
                    .build()
            )
        ).withMessageContaining("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant 2");
    }

    @Test
    void map_medIngenLovvalgperioder_kastFunksjonellException() {
        mockHappyCase();
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong()))
            .thenReturn(List.of());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() ->
                trygdeavtaleAttestMapper.map(new DokgenBrevbestilling.Builder()
                    .medBehandling(lagBehandling())
                    .medPersonDokument(lagPersonDokument())
                    .medVedtaksdato(Instant.parse("1970-10-10T00:00:00Z"))
                    .build()
            )
        ).withMessageContaining("Det kan bare være en lovvalgsperiode for trygdeavtale. Fant 0");
    }

    private void mockHappyCase() {
        when(mockPersondata.getFødselsdato()).thenReturn(LocalDate.of(1970, 1, 1));
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagAvklartMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagAvklartMedfølgendeBarn());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(anyLong())).thenReturn(lagMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedfølgendeBarn());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(mockPersondata);
        when(mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any())).thenReturn(lagAvklarteVirksomheter());
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(List.of(lagLovvalgsperiode()));
        when(mockDokgenMapperDatahenter.hentSammensattNavn(anyString())).thenAnswer((Answer<String>) invocationOnMock -> {
            String fnr = invocationOnMock.getArgument(0);
            String navn = null;
            if (fnr.equals(EKTEFELLE_FNR)) {
                navn = EKTEFELLE_NAVN;
            } else if (fnr.equals(BARN1_FNR)) {
                navn = BARN1_NAVN;
            }
            return navn;
        });
    }

    private Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.of(2020, 1, 1));
        lovvalgsperiode.setTom(LocalDate.of(2021, 1, 1));
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        return lovvalgsperiode;
    }

    private List<AvklartVirksomhet> lagAvklarteVirksomheter() {
        return List.of(new AvklartVirksomhet(ARBEIDSGIVER_NAVN, ORG_NR, BrevDataTestUtils.lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID));
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeEktefelle() {
        OmfattetFamilie ektefelle = new OmfattetFamilie(UUID_EKTEFELLE);
        return new AvklarteMedfolgendeFamilie(Set.of(ektefelle), Set.of());
    }

    private AvklarteMedfolgendeBarn lagAvklartMedfølgendeBarn() {
        return new AvklarteMedfolgendeBarn(Set.of(new OmfattetFamilie(UUID_BARN_1)), Set.of());
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeEktefelle() {
        MedfolgendeFamilie ektefelle = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER);
        return Map.of(UUID_EKTEFELLE, ektefelle);
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeBarn() {
        MedfolgendeFamilie medfolgendeBarn1 = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_1, BARN1_FNR, BARN1_NAVN, MedfolgendeFamilie.Relasjonsrolle.BARN);
        return Map.of(UUID_BARN_1, medfolgendeBarn1);
    }


}
