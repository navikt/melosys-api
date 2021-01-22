package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.familie.OmfattetFamilie;
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
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
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
        Set<OmfattetFamilie> omfattetBarn = Set.of(new OmfattetFamilie("uuid1"));
        Set<IkkeOmfattetFamilie> ikkeOmfattetBarns = Set.of(new IkkeOmfattetFamilie("uuid2", "OVER_18_AR", "fritekstForUuid2"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn = new AvklarteMedfolgendeFamilie(omfattetBarn, ikkeOmfattetBarns);

        Set<OmfattetFamilie> omfattetEktefelleSamboer = Set.of(new OmfattetFamilie("uuid3"));
        Set<IkkeOmfattetFamilie> ikkeOmfattetEktefelleSamboers = Set.of(new IkkeOmfattetFamilie("uuid4", "SAMBOER_UTEN_FELLES_BARN", "fritekstForUuid4"));
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer = new AvklarteMedfolgendeFamilie(omfattetEktefelleSamboer, ikkeOmfattetEktefelleSamboers);

        MedfolgendeFamilieDto medfolgendeFamilieDto = new MedfolgendeFamilieDto(avklarteMedfolgendeBarn, avklarteMedfolgendeEktefelleSamboer);

        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(Set.of(
            lagAvklartefaktaDto("uuid1", Avklartefaktatyper.VURDERING_LOVVALG_BARN, true, "", ""),
            lagAvklartefaktaDto("uuid2", Avklartefaktatyper.VURDERING_LOVVALG_BARN, false, "fritekstForUuid2", "OVER_18_AR"),
            lagAvklartefaktaDto("uuid3", Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, true, "", ""),
            lagAvklartefaktaDto("uuid4", Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, false, "fritekstForUuid4", "SAMBOER_UTEN_FELLES_BARN")));

        AvklartefaktaOppsummeringDto response = avklartefaktaTjeneste.lagreMedfolgendeFamilieSomAvklarteFakta(1L, medfolgendeFamilieDto);

        Set<OmfattetFamilie> responseOmfattetBarn = response.getMedfolgendeFamilie().getAvklarteMedfolgendeBarn().getFamilieOmfattetAvNorskTrygd();
        assertEquals(responseOmfattetBarn.size(), 1);
        assertEquals(responseOmfattetBarn.iterator().next().uuid, omfattetBarn.iterator().next().uuid);

        Set<IkkeOmfattetFamilie> responseIkkeOmfattetBarn = response.getMedfolgendeFamilie().getAvklarteMedfolgendeBarn().getFamilieIkkeOmfattetAvNorskTrygd();
        assertEquals(responseIkkeOmfattetBarn.size(), 1);
        assertEquals(responseIkkeOmfattetBarn.iterator().next().uuid, ikkeOmfattetBarns.iterator().next().uuid);
        assertEquals(responseIkkeOmfattetBarn.iterator().next().begrunnelse, ikkeOmfattetBarns.iterator().next().begrunnelse);
        assertEquals(responseIkkeOmfattetBarn.iterator().next().begrunnelseFritekst, ikkeOmfattetBarns.iterator().next().begrunnelseFritekst);

        Set<OmfattetFamilie> responseOmfattetEktefelleSamboer = response.getMedfolgendeFamilie().getAvklarteMedfolgendeEktefelleSamboer().getFamilieOmfattetAvNorskTrygd();
        assertEquals(responseOmfattetEktefelleSamboer.size(), 1);
        assertEquals(responseOmfattetEktefelleSamboer.iterator().next().uuid, omfattetEktefelleSamboer.iterator().next().uuid);

        Set<IkkeOmfattetFamilie> responseIkkeOmfattetEktefelleSamboer = response.getMedfolgendeFamilie().getAvklarteMedfolgendeEktefelleSamboer().getFamilieIkkeOmfattetAvNorskTrygd();
        assertEquals(responseIkkeOmfattetEktefelleSamboer.size(), 1);
        assertEquals(responseIkkeOmfattetEktefelleSamboer.iterator().next().uuid, ikkeOmfattetEktefelleSamboers.iterator().next().uuid);
        assertEquals(responseIkkeOmfattetEktefelleSamboer.iterator().next().begrunnelse, ikkeOmfattetEktefelleSamboers.iterator().next().begrunnelse);
        assertEquals(responseIkkeOmfattetEktefelleSamboer.iterator().next().begrunnelseFritekst, ikkeOmfattetEktefelleSamboers.iterator().next().begrunnelseFritekst);
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
            avklartefakta.setFakta("TRUE");
        } else {
            avklartefakta.setFakta("FALSE");
            AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
            registrering.setAvklartefakta(avklartefakta);
            registrering.setBegrunnelseKode(begrunnelsekode);
            avklartefakta.setRegistreringer(new HashSet<>(List.of(registrering)));
        }
        return new AvklartefaktaDto(avklartefakta);
    }
}

