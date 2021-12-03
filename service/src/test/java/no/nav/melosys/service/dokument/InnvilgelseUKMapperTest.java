package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.familie.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.InnvilgelseUK;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_UK;
import static no.nav.melosys.service.dokument.DokgenTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InnvilgelseUKMapperTest {
    public static final String UUID_EKTEFELLE = "uuidEktefelle";
    public static final String UUID_BARN_1 = "uuidBarn1";
    public static final String UUID_BARN_2 = "uuidBarn2";
    public static final String EKTEFELLE_FNR = "09080723451";
    private static final String BARN1_FNR = "12131456789";
    private static final String BARN2_FNR = "12151456789";
    public static final String SAKSNUMMER = "MEL-123";
    public static final String EKTEFELLE_NAVN = "Dolly Duck";
    public static final String BARN_NAVN_1 = "Doffen Duck";
    public static final String BARN_NAVN_2 = "Dole Duck";

    @Mock
    private AvklarteMedfolgendeFamilieService mockAvklarteMedfolgendeFamilieService;

    @Mock
    private DokgenMapperDatahenter mockDokgenMapperDatahenter;

    @Mock
    private LovvalgsperiodeService mockLovvalgsperiodeService;

    @Mock
    PersondataFasade mockPersondataFasade;

    @Mock
    Persondata mockPersondata;

    private InnvilgelseUKMapper innvilgelseUKMapper;

    @BeforeEach
    void setup() {
        innvilgelseUKMapper = new InnvilgelseUKMapper(
            mockAvklarteMedfolgendeFamilieService,
            mockDokgenMapperDatahenter,
            mockLovvalgsperiodeService,
            mockPersondataFasade);

    }

    @Test
    void map_Innvilget_populererFelter() throws JsonProcessingException {
        mockData();
        when(mockDokgenMapperDatahenter.hentSammensattNavn(BARN1_FNR)).thenReturn(BARN_NAVN_1);
        when(mockDokgenMapperDatahenter.hentSammensattNavn(BARN2_FNR)).thenReturn(BARN_NAVN_2);

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling();

        InnvilgelseUK innvilgelseUK = innvilgelseUKMapper.map(brevbestilling);

        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(innvilgelseUK);
        String resultat = json.replaceAll("(\"dagensDato\" :)(.*)", "$1 \"Fjernet for test\",");

        assertThat(resultat).isEqualToIgnoringNewLines(FORVENTEDE_FELTER_FOR_INNVIGLESE_STORBRITANNIA_MAPPING);
    }

    @Test
    void map_ingenUtenlandskeVirksomheter_kastFunksjonellException() {
        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling(lagBehandlingMedPeriode());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> innvilgelseUKMapper.map(brevbestilling))
            .withMessageContaining("Ingen utenlandske virksomheter funnet");
    }

    @Test
    void map_ettOmfattetBarn_minstEttOmfattetFamiliemedlemErtrue() {
        mockData();
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagOmfatetMedfølgendeBarn());
        when(mockDokgenMapperDatahenter.hentSammensattNavn(BARN1_FNR)).thenReturn(BARN_NAVN_1);

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling();
        InnvilgelseUK map = innvilgelseUKMapper.map(brevbestilling);
        assertThat(map.getFamilie().minstEttOmfattetFamiliemedlem()).isTrue();
    }

    @Test
    void map_ingenOmfattet_minstEttOmfattetFamiliemedlemErfalse() {
        mockData();
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagIkkeOmfatetMedfølgendeBarn());
        when(mockDokgenMapperDatahenter.hentSammensattNavn(BARN1_FNR)).thenReturn(BARN_NAVN_1);

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling();
        InnvilgelseUK map = innvilgelseUKMapper.map(brevbestilling);
        assertThat(map.getFamilie().minstEttOmfattetFamiliemedlem()).isFalse();
    }

    private void mockData() {
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(List.of(lagLovvalgsperiode()));
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagAvklartMedfølgendeBarn());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(anyLong())).thenReturn(lagMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedfølgendeBarn());
        when(mockPersondata.getFødselsdato()).thenReturn(LocalDate.of(1970, 1, 1));
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(mockPersondata);
        when(mockDokgenMapperDatahenter.hentSammensattNavn(EKTEFELLE_FNR)).thenReturn(EKTEFELLE_NAVN);
    }

    private Behandling lagBehandlingMedPeriode() {
        var behandling = lagBehandling(lagFagsak(true));
        var behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        behandlingsgrunnlag.setMottaksdato(LocalDate.of(2019, 10, 1));
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().periode = new Periode(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2021, 1, 1)
        );
        return behandling;
    }

    private Behandling leggPåForetakUtland(Behandling behandling) {
        var behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        var foretakUtland = new ForetakUtland();
        foretakUtland.navn = "Foretaksnavn";
        var adresse = new StrukturertAdresse();
        adresse.setLandkode(Landkoder.GB.getKode());
        foretakUtland.adresse = adresse;
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().foretakUtland = List.of(foretakUtland);
        return behandling;
    }

    private InnvilgelseBrevbestilling lagInnvilgelseBrevbestilling() {
        var behandling = lagBehandlingMedPeriode();
        return lagInnvilgelseBrevbestilling(leggPåForetakUtland(behandling));
    }

    private InnvilgelseBrevbestilling lagInnvilgelseBrevbestilling(Behandling behandling) {

        return new InnvilgelseBrevbestilling.Builder()
            .medProduserbartdokument(INNVILGELSE_UK)
            .medPersonDokument(lagPersonDokument())
            .medBehandling(behandling)
            .medOrg(lagOrg())
            .medKontaktopplysning(lagKontaktOpplysning())
            .medForsendelseMottatt(Instant.now())
            .medInnledningFritekst("innledningFritekst")
            .medBegrunnelseFritekst("begrunnelse")
            .medBarnFritekst("barnFritekst")
            .medEktefelleFritekst("ektefelleFritekst")
            .build();
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.of(2020, 1, 1));
        lovvalgsperiode.setTom(LocalDate.of(2021, 1, 1));
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        return lovvalgsperiode;
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

    private Map<String, MedfolgendeFamilie> lagMedfølgendeBarn() {
        return Map.of(
            UUID_BARN_1,
            MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN_1, BARN1_FNR, BARN_NAVN_1, MedfolgendeFamilie.Relasjonsrolle.BARN),
            UUID_BARN_2,
            MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN_2, BARN2_FNR, BARN_NAVN_2, MedfolgendeFamilie.Relasjonsrolle.BARN)
        );
    }

    private AvklarteMedfolgendeBarn lagOmfatetMedfølgendeBarn() {
        var barn = new OmfattetFamilie(UUID_BARN_1);
        barn.setSammensattNavn(BARN_NAVN_1);
        barn.setIdent(BARN1_FNR);
        return new AvklarteMedfolgendeBarn(
            Set.of(barn),
            Set.of());
    }

    private AvklarteMedfolgendeBarn lagIkkeOmfatetMedfølgendeBarn() {
        var barn = new IkkeOmfattetBarn(
            UUID_BARN_1,
            Medfolgende_barn_begrunnelser.MANGLER_OPPLYSNINGER.getKode(), "");
        return new AvklarteMedfolgendeBarn(
            Set.of(),
            Set.of(barn));
    }


    private AvklarteMedfolgendeBarn lagAvklartMedfølgendeBarn() {
        var b1 = new OmfattetFamilie(UUID_BARN_1);
        b1.setSammensattNavn(BARN_NAVN_1);
        b1.setIdent(BARN1_FNR);
        var b2 = new IkkeOmfattetBarn(
            UUID_BARN_2,
            Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
            null);
        b2.sammensattNavn = BARN_NAVN_2;
        b2.ident = BARN2_FNR;
        return new AvklarteMedfolgendeBarn(
            Set.of(b1),
            Set.of(b2));
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeEktefelle() {
        var ektefelle = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER);
        return Map.of(UUID_EKTEFELLE, ektefelle);
    }

    private static final String FORVENTEDE_FELTER_FOR_INNVIGLESE_STORBRITANNIA_MAPPING = """
        {
          "saksopplysninger" : {
            "saksnummer" : "MEL-123",
            "navnBruker" : "Donald Duck",
            "fnr" : "05058892382"
          },
          "dagensDato" : "Fjernet for test",
          "mottaker" : {
            "navn" : "Advokatene AS",
            "adresselinjer" : [ "Att: Fetter Anton", "POSTBOKS 200" ],
            "postnr" : "9990",
            "poststed" : null,
            "land" : null,
            "type" : "BRUKER"
          },
          "innvilgelse" : {
            "innledningFritekst" : "innledningFritekst",
            "begrunnelseFritekst" : "begrunnelse",
            "ektefelleFritekst" : "ektefelleFritekst",
            "barnFritekst" : "barnFritekst"
          },
          "artikkel" : "UK_ART6_1",
          "soknad" : {
            "soknadsdato" : "2019-10-01",
            "periodeFom" : "2020-01-01",
            "periodeTom" : "2021-01-01",
            "virksomhetsnavn" : "Foretaksnavn"
          },
          "familie" : {
            "minstEttOmfattetFamiliemedlem" : true,
            "ektefelle" : {
              "navn" : "Dolly Duck",
              "omfattet" : true,
              "begrunnelse" : null,
              "fnr" : "09080723451",
              "dnr" : null,
              "foedselsdato" : "1970-01-01"
            },
            "barn" : [ {
              "navn" : "Doffen Duck",
              "omfattet" : true,
              "begrunnelse" : null,
              "fnr" : "12131456789",
              "dnr" : null,
              "foedselsdato" : "1970-01-01"
            }, {
              "navn" : "Dole Duck",
              "omfattet" : false,
              "begrunnelse" : "OVER_18_AR",
              "fnr" : "12151456789",
              "dnr" : null,
              "foedselsdato" : "1970-01-01"
            } ]
          },
          "virksomhetArbeidsgiverSkalHaKopi" : false
        }""";
}
