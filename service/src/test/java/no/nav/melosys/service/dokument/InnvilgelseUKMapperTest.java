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
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetBarn;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
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
    public static final String ARBEIDSGIVER_NAVN = "Bang Hansen";
    public static final String SAKSNUMMER = "MEL-123";
    public static final String EKTEFELLE_NAVN = "Dolly Duck";
    public static final String BARN_NAVN_1 = "Doffen Duck";
    public static final String BARN_NAVN_2 = "Dole Duck";
    public static final String ORG_NR = "987654321";
    public static final Instant VEDTAKS_DATO = Instant.parse("1970-10-10T00:00:00Z");

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
        innvilgelseUKMapper = new InnvilgelseUKMapper(mockAvklarteMedfolgendeFamilieService,
            mockDokgenMapperDatahenter,
            mockLovvalgsperiodeService,
            mockPersondataFasade);
    }

    @Test
    void test() throws JsonProcessingException {
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(List.of(lagLovvalgsperiode()));

        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagAvklartMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagAvklartMedfølgendeBarn());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(anyLong())).thenReturn(lagMedfølgendeEktefelle());
//        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedfølgendeBarn());
        when(mockPersondata.getFødselsdato()).thenReturn(LocalDate.of(1970, 1, 1));
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(mockPersondata);

        Behandling behandling = lagBehandling(lagFagsak(true));
        behandling.getBehandlingsgrunnlag().setMottaksdato(LocalDate.of(2019, 10, 1));
        behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().periode = new Periode(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2021, 1, 1)
        );
        InnvilgelseBrevbestilling brevbestilling = new InnvilgelseBrevbestilling.Builder()
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

        InnvilgelseUK innvilgelseUK = innvilgelseUKMapper.map(brevbestilling);
        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(innvilgelseUK);
        System.out.println(json);
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.of(2020, 1, 1));
        lovvalgsperiode.setTom(LocalDate.of(2021, 1, 1));
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        return lovvalgsperiode;
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeEktefelle() {
        OmfattetFamilie ektefelle = new OmfattetFamilie(UUID_EKTEFELLE);
        return new AvklarteMedfolgendeFamilie(Set.of(ektefelle), Set.of());
    }

    private AvklarteMedfolgendeBarn lagAvklartMedfølgendeBarn() {
        OmfattetFamilie b1 = new OmfattetFamilie(UUID_BARN_1);
        b1.setSammensattNavn("Barn 1");
        b1.setIdent("fnr1");
        IkkeOmfattetBarn b2 = new IkkeOmfattetBarn(UUID_BARN_2, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(), "begrunnelseFritekst");
        b2.sammensattNavn = "Barn 2";
        b2.ident = "fnr2";
        return new AvklarteMedfolgendeBarn(
            Set.of(b1),
            Set.of(b2));
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeEktefelle() {
        MedfolgendeFamilie ektefelle = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, MedfolgendeFamilie.Relasjonsrolle.EKTEFELLE_SAMBOER);
        return Map.of(UUID_EKTEFELLE, ektefelle);
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeBarn() {
        MedfolgendeFamilie medfolgendeBarn1 = MedfolgendeFamilie.tilMedfolgendeFamilie(
            UUID_BARN_1, BARN1_FNR, BARN_NAVN_1, MedfolgendeFamilie.Relasjonsrolle.BARN);
        return Map.of(UUID_BARN_1, medfolgendeBarn1);
    }

}
