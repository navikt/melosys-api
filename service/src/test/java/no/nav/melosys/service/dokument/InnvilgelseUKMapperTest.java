package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.Barn;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.InnvilgelseUK;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.INNVILGELSE_UK;
import static no.nav.melosys.service.dokument.DokgenMalMapperTest.*;
import static no.nav.melosys.service.dokument.DokgenTestData.*;
import static no.nav.melosys.service.dokument.DokgenTrygdeavtaleTestData.lagLovvalgsperiode;
import static no.nav.melosys.service.dokument.DokgenTrygdeavtaleTestData.lagTrygdeavtaleBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InnvilgelseUKMapperTest {
    private static final String UUID_EKTEFELLE = "uuidEktefelle";
    private static final String UUID_BARN_1 = "uuidBarn1";
    private static final String UUID_BARN_2 = "uuidBarn2";
    private static final String UUID_BARN_3 = "uuidBarn3";
    private static final String EKTEFELLE_FNR = "01108049800";
    private static final String BARN1_FNR = "01100099728";
    private static final String BARN2_FNR = "02109049878";
    private static final String BARN_NAVN_1 = "Doffen Duck";
    private static final String BARN_NAVN_3 = "Utenid Duck";
    private static final String BARN3_UTEN_FNR = "01.02.2021";

    @Mock
    private AvklarteMedfolgendeFamilieService mockAvklarteMedfolgendeFamilieService;

    @Mock
    private LovvalgsperiodeService mockLovvalgsperiodeService;

    private InnvilgelseUKMapper innvilgelseUKMapper;

    @BeforeEach
    void setup() {
        innvilgelseUKMapper = new InnvilgelseUKMapper(
            mockAvklarteMedfolgendeFamilieService,
            mockLovvalgsperiodeService);
    }

    @Test
    void map_Innvilget_populererFelter() throws JsonProcessingException {
        mockLovvalgsperiode();
        mockMedfølgendeFamilieDefaultCase();
        mockAvklartFamilieDefaultCase();

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling(medPeriode(lagTrygdeavtaleBehandling()));

        InnvilgelseUK innvilgelseUK = innvilgelseUKMapper.map(brevbestilling);

        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(innvilgelseUK);
        String resultat = json.replaceAll("(\"dagensDato\" :)(.*)", "$1 \"Fjernet for test\",");

        assertThat(resultat).isEqualToIgnoringNewLines(FORVENTEDE_FELTER_FOR_INNVILGELSE_STORBRITANNIA_MAPPING);
    }

    @Test
    void map_ingenUtenlandskeVirksomheter_kastFunksjonellException() {
        mockLovvalgsperiode();
        Behandling behandling = medPeriode(lagTrygdeavtaleBehandling(null));
        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> innvilgelseUKMapper.map(brevbestilling))
            .withMessageContaining("Behandlingsgrunnlaget inneholder ikke representant I utlandet");
    }

    @Test
    void map_ettOmfattetBarn_minstEttOmfattetFamiliemedlemErtrue() {
        mockLovvalgsperiode();
        mockMedfølgendeFamilieDefaultCase();
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagOmfattetMedfølgendeBarn());

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling(lagTrygdeavtaleBehandling());
        InnvilgelseUK map = innvilgelseUKMapper.map(brevbestilling);
        assertThat(map.getFamilie().minstEttOmfattetFamiliemedlem()).isTrue();
    }

    @Test
    void map_barnUtenFnr_parseOppgittFnrTilDato() {
        mockLovvalgsperiode();
        mockMedfølgendeFamilieDefaultCase();
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(tomFamilie());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedølgendeBarnUtenFnr());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagBarnUtenFnr());

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling(lagTrygdeavtaleBehandling());
        InnvilgelseUK map = innvilgelseUKMapper.map(brevbestilling);
        assertThat(map.getFamilie().barn())
            .hasSize(1)
            .element(0)
            .extracting(Barn::fnr, Barn::foedselsdato)
            .containsExactly(null, LocalDate.of(2021, 02, 01));
    }

    @Test
    void map_ingenOmfattet_minstEttOmfattetFamiliemedlemErfalse() {
        mockLovvalgsperiode();
        mockMedfølgendeFamilieDefaultCase();
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeBarn());

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling(lagTrygdeavtaleBehandling());
        InnvilgelseUK map = innvilgelseUKMapper.map(brevbestilling);
        assertThat(map.getFamilie().minstEttOmfattetFamiliemedlem()).isFalse();
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
        when(mockLovvalgsperiodeService.hentValidertLovvalgsperiode(anyLong())).thenReturn(lagLovvalgsperiode());
    }

    private Behandling medPeriode(Behandling behandling) {
        var behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        behandlingsgrunnlag.setMottaksdato(SOKNADSDATO);
        return behandling;
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
            MedfolgendeFamilie.tilMedfolgendeFamilie(UUID_BARN_2, BARN2_FNR, "Dole Duck", MedfolgendeFamilie.Relasjonsrolle.BARN)
        );
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
        var ektefelle = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_EKTEFELLE, EKTEFELLE_FNR, "Dolly Duck", MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER);
        return Map.of(UUID_EKTEFELLE, ektefelle);
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

    private static final String FORVENTEDE_FELTER_FOR_INNVILGELSE_STORBRITANNIA_MAPPING = String.format("""
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
                "soknadsdato" : "%s",
                "periodeFom" : "%s",
                "periodeTom" : "%s",
                "virksomhetsnavn" : "Foretaksnavn"
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
}
