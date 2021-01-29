package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.tjenester.gui.dto.AvklartefaktaOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.LagreMedfolgendeFamilieDto;

import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AvklartefaktaTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(AvklartefaktaTjenesteTest.class);

    private static final String AVKLARTEFAKTA_SCHEMA = "avklartefakta-schema.json";

    private AvklartefaktaTjeneste avklartefaktaTjeneste;

    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private TilgangService tilgangService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;

    @Before
    public void setUp() {
        avklartefaktaTjeneste = new AvklartefaktaTjeneste(avklartefaktaService, tilgangService, avklarteVirksomheterService, avklarteMedfolgendeFamilieService);
    }

    @Test
    public void hentAvklartefakta() throws Exception {
        Set<AvklartefaktaDto> mockliste = defaultEasyRandom().objects(AvklartefaktaDto.class, 4).collect(Collectors.toSet());
        when(avklartefaktaService.hentAlleAvklarteFakta(1L)).thenReturn(mockliste);

        Set<AvklartefaktaDto> avklartefaktaDtoSet = avklartefaktaTjeneste.hentAvklarteFakta(1L);
        validerArray(avklartefaktaDtoSet, AVKLARTEFAKTA_SCHEMA, log);
    }

    @Test
    public void lagreAvklartefaktaGirKopiAvInput() throws Exception {
        Set<AvklartefaktaDto> avklartefaktaDtoer = defaultEasyRandom().objects(AvklartefaktaDto.class, 4).collect(Collectors.toSet());
        when(avklartefaktaService.hentAlleAvklarteFakta(1L)).thenReturn(avklartefaktaDtoer);
        Set<AvklartefaktaDto> resultat = avklartefaktaTjeneste.lagreAvklarteFakta(1, avklartefaktaDtoer);
        assertThat(resultat).isEqualTo(avklartefaktaDtoer);
    }

    @Test
    public void lagreMedfolgendeFamilieSomAvklarteFakta_énAvHverMuligInput_returnererKorrekt() throws FunksjonellException, TekniskException {
        LagreMedfolgendeFamilieDto lagreMedfolgendeFamilieDto = new LagreMedfolgendeFamilieDto(Set.of(
            new MedfolgendeFamilieDto("uuid1", true, null, null),
            new MedfolgendeFamilieDto("uuid2", false, OVER_18_AR.getKode(), "fritekstForUuid2"),
            new MedfolgendeFamilieDto("uuid3", true, null, null),
            new MedfolgendeFamilieDto("uuid4", false, SAMBOER_UTEN_FELLES_BARN.getKode(), "fritekstForUuid4")));

        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(Set.of(
            lagAvklartefaktaDto("uuid1", Avklartefaktatyper.VURDERING_LOVVALG_BARN, true, null, null),
            lagAvklartefaktaDto("uuid2", Avklartefaktatyper.VURDERING_LOVVALG_BARN, false, "fritekstForUuid2", OVER_18_AR.getKode()),
            lagAvklartefaktaDto("uuid3", Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, true, null, null),
            lagAvklartefaktaDto("uuid4", Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, false, "fritekstForUuid4", SAMBOER_UTEN_FELLES_BARN.getKode())));

        AvklartefaktaOppsummeringDto response = avklartefaktaTjeneste.lagreMedfolgendeFamilieSomAvklarteFakta(1L, lagreMedfolgendeFamilieDto);

        List<MedfolgendeFamilieDto> medFolgendeFamilieFraResponse = response.getMedfolgendeFamilie()
            .stream().sorted(Comparator.comparing(MedfolgendeFamilieDto::getUuid)).collect(Collectors.toList());
        List<MedfolgendeFamilieDto> forventetMedfolgendeFamilie = lagreMedfolgendeFamilieDto.getMedfolgendeFamilie()
            .stream().sorted(Comparator.comparing(MedfolgendeFamilieDto::getUuid)).collect(Collectors.toList());

        assertThat(medFolgendeFamilieFraResponse.size()).isEqualTo(forventetMedfolgendeFamilie.size());

        assertThat(medFolgendeFamilieFraResponse.get(0).getUuid()).isEqualTo(forventetMedfolgendeFamilie.get(0).getUuid());
        assertThat(medFolgendeFamilieFraResponse.get(0).getBegrunnelseKode()).isEqualTo(forventetMedfolgendeFamilie.get(0).getBegrunnelseKode());
        assertThat(medFolgendeFamilieFraResponse.get(0).getBegrunnelseFritekst()).isEqualTo(forventetMedfolgendeFamilie.get(0).getBegrunnelseFritekst());

        assertThat(medFolgendeFamilieFraResponse.get(1).getUuid()).isEqualTo(forventetMedfolgendeFamilie.get(1).getUuid());
        assertThat(medFolgendeFamilieFraResponse.get(1).getBegrunnelseKode()).isEqualTo(forventetMedfolgendeFamilie.get(1).getBegrunnelseKode());
        assertThat(medFolgendeFamilieFraResponse.get(1).getBegrunnelseFritekst()).isEqualTo(forventetMedfolgendeFamilie.get(1).getBegrunnelseFritekst());

        assertThat(medFolgendeFamilieFraResponse.get(2).getUuid()).isEqualTo(forventetMedfolgendeFamilie.get(2).getUuid());
        assertThat(medFolgendeFamilieFraResponse.get(2).getBegrunnelseKode()).isEqualTo(forventetMedfolgendeFamilie.get(2).getBegrunnelseKode());
        assertThat(medFolgendeFamilieFraResponse.get(2).getBegrunnelseFritekst()).isEqualTo(forventetMedfolgendeFamilie.get(2).getBegrunnelseFritekst());

        assertThat(medFolgendeFamilieFraResponse.get(3).getUuid()).isEqualTo(forventetMedfolgendeFamilie.get(3).getUuid());
        assertThat(medFolgendeFamilieFraResponse.get(3).getBegrunnelseKode()).isEqualTo(forventetMedfolgendeFamilie.get(3).getBegrunnelseKode());
        assertThat(medFolgendeFamilieFraResponse.get(3).getBegrunnelseFritekst()).isEqualTo(forventetMedfolgendeFamilie.get(3).getBegrunnelseFritekst());
    }

    @Test(expected = FunksjonellException.class)
    public void lagreAvklartefakta_ikkeRedigerbarBehandling_girFeil() throws FunksjonellException, TekniskException {
        doThrow(FunksjonellException.class).when(tilgangService).sjekkRedigerbarOgTilgang(anyLong());

        avklartefaktaTjeneste.lagreAvklarteFakta(1, Collections.emptySet());
    }

    @Test(expected = SikkerhetsbegrensningException.class)
    public void hentAvklartefakta_ikkeTilgang_girFeil() throws FunksjonellException, TekniskException {
        doThrow(SikkerhetsbegrensningException.class).when(tilgangService).sjekkTilgang(anyLong());

        avklartefaktaTjeneste.hentAvklarteFakta(1);
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

