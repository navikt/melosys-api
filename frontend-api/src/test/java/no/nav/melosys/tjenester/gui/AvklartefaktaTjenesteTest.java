package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.tilgang.TilgangService;
import no.nav.melosys.tjenester.gui.dto.AvklartefaktaOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.LagreMedfolgendeFamilieDto;
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvklartefaktaTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(AvklartefaktaTjenesteTest.class);

    private static final String AVKLARTEFAKTA_SCHEMA = "avklartefakta-schema.json";
    private static final String AVKLARTEFAKTA_MEDFOLGENDEFAMILIE_POST_SCHEMA="avklartefakta-medfolgendefamilie-post-schema.json";
    private static final String AVKLARTEFAKTA_OPPSUMMERING_SCHEMA="avklartefakta-oppsummering-schema.json";

    private static final String uuid1 = "36053ce6-75e5-4430-b8af-2ce60092877d";
    private static final String uuid2 = "e502441e-9cdd-4d2a-84c2-25261b6e7cb2";
    private static final String uuid3 = "d7645947-e7e9-46c0-987a-d0e91d6fed6f";
    private static final String uuid4 = "4136cdce-0c09-4693-a032-5914575c3ac3";

    private AvklartefaktaTjeneste avklartefaktaTjeneste;

    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private TilgangService tilgangService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;

    @BeforeEach
    public void setUp() {
        avklartefaktaTjeneste = new AvklartefaktaTjeneste(avklartefaktaService, tilgangService, avklarteVirksomheterService, avklarteMedfolgendeFamilieService);
    }

    @Test
    void hentAvklartefakta() throws Exception {
        Set<AvklartefaktaDto> mockliste = defaultEasyRandom().objects(AvklartefaktaDto.class, 4).collect(Collectors.toSet());
        when(avklartefaktaService.hentAlleAvklarteFakta(1L)).thenReturn(mockliste);

        Set<AvklartefaktaDto> avklartefaktaDtoSet = avklartefaktaTjeneste.hentAvklarteFakta(1L);
        validerArray(avklartefaktaDtoSet, AVKLARTEFAKTA_SCHEMA, log);
    }

    @Test
    void lagreAvklartefaktaGirKopiAvInput() {
        Set<AvklartefaktaDto> avklartefaktaDtoer = defaultEasyRandom().objects(AvklartefaktaDto.class, 4).collect(Collectors.toSet());
        when(avklartefaktaService.hentAlleAvklarteFakta(1L)).thenReturn(avklartefaktaDtoer);
        Set<AvklartefaktaDto> resultat = avklartefaktaTjeneste.lagreAvklarteFakta(1, avklartefaktaDtoer);
        assertThat(resultat).isEqualTo(avklartefaktaDtoer);
    }

    @Test
    void lagreMedfolgendeFamilieSomAvklarteFakta_énAvHverMuligInput_returnererKorrekt() throws IOException {
        LagreMedfolgendeFamilieDto lagreMedfolgendeFamilieDto = new LagreMedfolgendeFamilieDto(Set.of(
            new MedfolgendeFamilieDto(uuid1, true, null, null),
            new MedfolgendeFamilieDto(uuid2, false, OVER_18_AR.getKode(), "fritekstForUuid2"),
            new MedfolgendeFamilieDto(uuid3, true, null, null),
            new MedfolgendeFamilieDto(uuid4, false, SAMBOER_UTEN_FELLES_BARN.getKode(), "fritekstForUuid4")));

        valider(lagreMedfolgendeFamilieDto, AVKLARTEFAKTA_MEDFOLGENDEFAMILIE_POST_SCHEMA, log);

        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(Set.of(
            lagAvklartefaktaDto(uuid1, Avklartefaktatyper.VURDERING_LOVVALG_BARN, true, null, null),
            lagAvklartefaktaDto(uuid2, Avklartefaktatyper.VURDERING_LOVVALG_BARN, false, "fritekstForUuid2", OVER_18_AR.getKode()),
            lagAvklartefaktaDto(uuid3, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, true, null, null),
            lagAvklartefaktaDto(uuid4, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, false, "fritekstForUuid4", SAMBOER_UTEN_FELLES_BARN.getKode())));

        AvklartefaktaOppsummeringDto response = avklartefaktaTjeneste.lagreMedfolgendeFamilieSomAvklarteFakta(1L, lagreMedfolgendeFamilieDto);

        valider(response, AVKLARTEFAKTA_OPPSUMMERING_SCHEMA, log);

        List<MedfolgendeFamilieDto> medFolgendeFamilieFraResponse = response.getMedfolgendeFamilie()
            .stream().sorted(Comparator.comparing(MedfolgendeFamilieDto::uuid)).collect(Collectors.toList());
        List<MedfolgendeFamilieDto> forventetMedfolgendeFamilie = lagreMedfolgendeFamilieDto.medfolgendeFamilie()
            .stream().sorted(Comparator.comparing(MedfolgendeFamilieDto::uuid)).collect(Collectors.toList());

        assertThat(medFolgendeFamilieFraResponse.size()).isEqualTo(forventetMedfolgendeFamilie.size());

        assertThat(medFolgendeFamilieFraResponse.get(0).uuid()).isEqualTo(forventetMedfolgendeFamilie.get(0).uuid());
        assertThat(medFolgendeFamilieFraResponse.get(0).begrunnelseKode()).isEqualTo(forventetMedfolgendeFamilie.get(0).begrunnelseKode());
        assertThat(medFolgendeFamilieFraResponse.get(0).begrunnelseFritekst()).isEqualTo(forventetMedfolgendeFamilie.get(0).begrunnelseFritekst());

        assertThat(medFolgendeFamilieFraResponse.get(1).uuid()).isEqualTo(forventetMedfolgendeFamilie.get(1).uuid());
        assertThat(medFolgendeFamilieFraResponse.get(1).begrunnelseKode()).isEqualTo(forventetMedfolgendeFamilie.get(1).begrunnelseKode());
        assertThat(medFolgendeFamilieFraResponse.get(1).begrunnelseFritekst()).isEqualTo(forventetMedfolgendeFamilie.get(1).begrunnelseFritekst());

        assertThat(medFolgendeFamilieFraResponse.get(2).uuid()).isEqualTo(forventetMedfolgendeFamilie.get(2).uuid());
        assertThat(medFolgendeFamilieFraResponse.get(2).begrunnelseKode()).isEqualTo(forventetMedfolgendeFamilie.get(2).begrunnelseKode());
        assertThat(medFolgendeFamilieFraResponse.get(2).begrunnelseFritekst()).isEqualTo(forventetMedfolgendeFamilie.get(2).begrunnelseFritekst());

        assertThat(medFolgendeFamilieFraResponse.get(3).uuid()).isEqualTo(forventetMedfolgendeFamilie.get(3).uuid());
        assertThat(medFolgendeFamilieFraResponse.get(3).begrunnelseKode()).isEqualTo(forventetMedfolgendeFamilie.get(3).begrunnelseKode());
        assertThat(medFolgendeFamilieFraResponse.get(3).begrunnelseFritekst()).isEqualTo(forventetMedfolgendeFamilie.get(3).begrunnelseFritekst());
    }

    private static AvklartefaktaDto lagAvklartefaktaDto(String subjektID, Avklartefaktatyper type, boolean fakta, String begrunnelseFritekst, String begrunnelsekode) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setSubjekt(subjektID);
        avklartefakta.setType(type);
        avklartefakta.setReferanse(type.getKode());
        avklartefakta.setBegrunnelseFritekst(begrunnelseFritekst);
        if (fakta) {
            avklartefakta.setFakta(Avklartefakta.VALGT_FAKTA);
        } else {
            avklartefakta.setFakta(Avklartefakta.IKKE_VALGT_FAKTA);
            AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
            registrering.setAvklartefakta(avklartefakta);
            registrering.setBegrunnelseKode(begrunnelsekode);
            avklartefakta.setRegistreringer(new HashSet<>(List.of(registrering)));
        }
        return new AvklartefaktaDto(avklartefakta);
    }
}

