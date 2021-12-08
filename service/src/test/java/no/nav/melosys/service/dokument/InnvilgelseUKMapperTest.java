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
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.familie.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelsestorbritannia.Barn;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InnvilgelseUKMapperTest {
    private static final String UUID_EKTEFELLE = "uuidEktefelle";
    private static final String UUID_BARN_1 = "uuidBarn1";
    private static final String UUID_BARN_2 = "uuidBarn2";
    private static final String UUID_BARN_3 = "uuidBarn3";
    private static final String EKTEFELLE_FNR = "01108049800";
    private static final LocalDate EKTEFELLE_FOEDSELSDATO = LocalDate.of(1980, 10, 2);
    private static final String BARN1_FNR = "01100099728";
    private static final LocalDate BARN1_FOEDSELSDATO = LocalDate.of(2000, 10, 1);
    private static final String BARN2_FNR = "02109049878";
    private static final LocalDate BARN2_FOEDSELSDATO = LocalDate.of(1990, 10, 2);
    private static final String EKTEFELLE_NAVN = "Dolly Duck";
    private static final String BARN_NAVN_1 = "Doffen Duck";
    private static final String BARN_NAVN_2 = "Dole Duck";
    private static final String BARN_NAVN_3 = "Utenid Duck";
    private static final String BARN3_UTEN_FNR = "01.02.2021";
    private static final LocalDate BARN3_FOEDSELSDATO = LocalDate.of(2021, 02, 01);
    private static final LocalDate FRA_DATO = LocalDate.of(2020, 1, 1);
    private static final LocalDate TIL_DATO = LocalDate.of(2021, 1, 1);
    private static final LocalDate SOKNADSDATO = LocalDate.of(2019, 10, 1);

    private static final Map<String, LocalDate> FNR_TIL_DATO = Map.of(
        EKTEFELLE_FNR, EKTEFELLE_FOEDSELSDATO,
        BARN1_FNR, BARN1_FOEDSELSDATO,
        BARN2_FNR, BARN2_FOEDSELSDATO
    );

    @Mock
    private AvklarteMedfolgendeFamilieService mockAvklarteMedfolgendeFamilieService;

    @Mock
    private LovvalgsperiodeService mockLovvalgsperiodeService;

    @Mock
    private PersondataFasade mockPersondataFasade;

    private InnvilgelseUKMapper innvilgelseUKMapper;

    @BeforeEach
    void setup() {
        innvilgelseUKMapper = new InnvilgelseUKMapper(
            mockAvklarteMedfolgendeFamilieService,
            mockLovvalgsperiodeService,
            mockPersondataFasade);

    }

    @Test
    void map_Innvilget_populererFelter() throws JsonProcessingException {
        mockLovvalgsperiode();
        mockMedfølgendeFamilieDefaultCase();
        mockAvklartFamilieDefaultCase();
        mockPerson(EKTEFELLE_FNR, BARN1_FNR, BARN2_FNR);

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling();

        InnvilgelseUK innvilgelseUK = innvilgelseUKMapper.map(brevbestilling);

        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(innvilgelseUK);
        String resultat = json.replaceAll("(\"dagensDato\" :)(.*)", "$1 \"Fjernet for test\",");

        assertThat(resultat).isEqualToIgnoringNewLines(FORVENTEDE_FELTER_FOR_INNVILGELSE_STORBRITANNIA_MAPPING);
    }

    @Test
    void map_ingenUtenlandskeVirksomheter_kastFunksjonellException() {
        mockLovvalgsperiode();
        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling(lagBehandlingMedPeriode());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> innvilgelseUKMapper.map(brevbestilling))
            .withMessageContaining("Ingen utenlandske virksomheter funnet");
    }

    @Test
    void map_ettOmfattetBarn_minstEttOmfattetFamiliemedlemErtrue() {
        mockLovvalgsperiode();
        mockMedfølgendeFamilieDefaultCase();
        mockPerson(EKTEFELLE_FNR, BARN1_FNR);
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagOmfatetMedfølgendeBarn());

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling();
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

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling();
        InnvilgelseUK map = innvilgelseUKMapper.map(brevbestilling);
        assertThat(map.getFamilie().barn())
            .hasSize(1)
            .element(0)
            .extracting(Barn::fnr, Barn::foedselsdato)
            .containsExactly(null, BARN3_FOEDSELSDATO);
    }

    @Test
    void map_ingenOmfattet_minstEttOmfattetFamiliemedlemErfalse() {
        mockLovvalgsperiode();
        mockMedfølgendeFamilieDefaultCase();
        mockPerson(EKTEFELLE_FNR, BARN1_FNR);
        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagIkkeOmfattetMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagIkkeOmfatetMedfølgendeBarn());

        InnvilgelseBrevbestilling brevbestilling = lagInnvilgelseBrevbestilling();
        InnvilgelseUK map = innvilgelseUKMapper.map(brevbestilling);
        assertThat(map.getFamilie().minstEttOmfattetFamiliemedlem()).isFalse();
    }

    private void mockPerson(String... personer) {
        for (var fnr : personer) {
            PersonDokument personDokument = new PersonDokument();
            personDokument.setFødselsdato(FNR_TIL_DATO.get(fnr));
            when(mockPersondataFasade.hentPerson(fnr)).thenReturn(personDokument);
        }
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

    private Behandling lagBehandlingMedPeriode() {
        var behandling = lagBehandling(lagFagsak(true));
        var behandlingsgrunnlag = behandling.getBehandlingsgrunnlag();
        behandlingsgrunnlag.setMottaksdato(SOKNADSDATO);
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().periode = new Periode(
            FRA_DATO,
            TIL_DATO
        );
        return behandling;
    }

    private Behandling medForetakUtland(Behandling behandling) {
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
        return lagInnvilgelseBrevbestilling(medForetakUtland(behandling));
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
        lovvalgsperiode.setFom(FRA_DATO);
        lovvalgsperiode.setTom(TIL_DATO);
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

    private AvklarteMedfolgendeFamilie lagOmfatetMedfølgendeBarn() {
        var barn = new OmfattetFamilie(UUID_BARN_1);
        barn.setSammensattNavn(BARN_NAVN_1);
        barn.setIdent(BARN1_FNR);
        return new AvklarteMedfolgendeFamilie(
            Set.of(barn),
            Set.of());
    }

    private AvklarteMedfolgendeFamilie lagIkkeOmfatetMedfølgendeBarn() {
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
            UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER);
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
        FRA_DATO,
        TIL_DATO,
        EKTEFELLE_FNR,
        EKTEFELLE_FOEDSELSDATO,
        BARN1_FNR,
        BARN1_FOEDSELSDATO,
        BARN2_FNR,
        BARN2_FOEDSELSDATO
    );
}
