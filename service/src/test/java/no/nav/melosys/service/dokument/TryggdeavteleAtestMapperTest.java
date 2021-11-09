package no.nav.melosys.service.dokument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.brev.storbritannia.AttestStorbritanniaBrevbestilling;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.person.Master;
import no.nav.melosys.domain.person.Persondata;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.integrasjon.dokgen.dto.atteststorbritannia.AttestStorbritannia;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static no.nav.melosys.service.dokument.DokgenTestData.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TryggdeavteleAtestMapperTest {
    public static final String UUID_EKTEFELLE = "uuidEktefelle";
    public static final String UUID_BARN_1 = "uuidBarn1";
    public static final String EKTEFELLE_FNR = "09080723451";
    private static final String BARN1_FNR = "12131456789";
    public static final String SAKSBEHANDLER_NAVN = "Fetter Anton";
    public static final String ARBEIDSGIVER_NAVN = "Bang Hansen";
    public static final String SAKSNUMMER = "MEL-123";
    public static final String EKTEFELLE_NAVN = "Dolly Duck";
    public static final String BARN1_NAVN = "Doffen Duck";
    public static final String REPRESENTANT_NAVN = "Representant AS";

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

    TryggdeavteleAtestMapper tryggdeavteleAtestMapper;

    @BeforeEach
    void setup() {
        tryggdeavteleAtestMapper = new TryggdeavteleAtestMapper(
            mockAvklarteMedfolgendeFamilieService,
            mockAvklarteVirksomheterService,
            mockDokgenMapperDatahenter,
            mockPersondataFasade,
            mockLovvalgsperiodeService);
    }

    @Test
    void map_InnvilgetMedOmfattetFamilie_populererFelter() {
        mockHappyCase();

        AttestStorbritannia attestStorbritannia = tryggdeavteleAtestMapper.map(new AttestStorbritanniaBrevbestilling.Builder()
            .medBehandling(lagBehandling())
            .medPersonDokument(lagPersonDokument())
            .medVedtaksdato(Instant.parse("1970-10-10T00:00:00Z"))
            .build()
        );

        // For nå bare skriv ut resultat så tester vi mot dette når mapping er ferdig
        try {
            String s = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(attestStorbritannia);
            System.out.println(s);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void mockHappyCase() {
        when(mockPersondata.getFødselsdato()).thenReturn(LocalDate.of(1970,1,1));

        when(mockAvklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(anyLong())).thenReturn(lagAvklartMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(anyLong())).thenReturn(lagAvklartMedfølgendeBarn());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(anyLong())).thenReturn(lagMedfølgendeEktefelle());
        when(mockAvklarteMedfolgendeFamilieService.hentMedfølgendeBarn(anyLong())).thenReturn(lagMedfølgendeBarn());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(mockPersondata);
        when(mockAvklarteVirksomheterService.hentNorskeArbeidsgivere(any())).thenReturn(lagAvklarteVirksomheter());
        when(mockLovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(lagreLovvalgsperioder());
    }

    private Collection<Lovvalgsperiode> lagreLovvalgsperioder() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.of(2020, 1, 1));
        lovvalgsperiode.setTom(LocalDate.of(2021, 1, 1));
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        return List.of(lovvalgsperiode);
    }

    private List<AvklartVirksomhet> lagAvklarteVirksomheter() {
        return List.of(new AvklartVirksomhet(ARBEIDSGIVER_NAVN, "987654321", BrevDataTestUtils.lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID));
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeEktefelle() {
        OmfattetFamilie ektefelle = new OmfattetFamilie(UUID_EKTEFELLE);
        return new AvklarteMedfolgendeFamilie(Set.of(ektefelle), Set.of());
    }

    private AvklarteMedfolgendeBarn lagAvklartMedfølgendeBarn() {
        return new AvklarteMedfolgendeBarn(Set.of(new OmfattetFamilie(UUID_BARN_1)), Set.of());
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeEktefelle() {
        MedfolgendeFamilie ektefelle = new MedfolgendeFamilie();
        ektefelle.fnr = EKTEFELLE_FNR;
        return Map.of(UUID_EKTEFELLE, ektefelle);
    }

    private Map<String, MedfolgendeFamilie> lagMedfølgendeBarn() {
        MedfolgendeFamilie medfolgendeBarn1 = new MedfolgendeFamilie();
        medfolgendeBarn1.fnr = BARN1_FNR;
        return Map.of(UUID_BARN_1, medfolgendeBarn1);
    }


}
