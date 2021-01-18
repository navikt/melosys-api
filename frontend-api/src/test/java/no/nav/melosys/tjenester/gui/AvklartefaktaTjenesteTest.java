package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeBarnFtrl;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeEktefelleSamboer;
import no.nav.melosys.domain.familie.IkkeOmfattetBarnFtrl;
import no.nav.melosys.domain.familie.IkkeOmfattetEktefelleSamboer;
import no.nav.melosys.domain.familie.OmfattetFamilie;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
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

    @Before
    public void setUp() {
        avklartefaktaTjeneste = new AvklartefaktaTjeneste(avklartefaktaService, tilgangService, avklarteVirksomheterService);
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
        Set<IkkeOmfattetBarnFtrl> ikkeOmfattetBarnFtrls = Set.of(new IkkeOmfattetBarnFtrl("uuid2", "OVER_18_AR", "fritekstForUuid2"));
        AvklarteMedfolgendeBarnFtrl avklarteMedfolgendeBarnFtrl = new AvklarteMedfolgendeBarnFtrl(omfattetBarn, ikkeOmfattetBarnFtrls);

        Set<OmfattetFamilie> omfattetEktefelleSamboer = Set.of(new OmfattetFamilie("uuid3"));
        Set<IkkeOmfattetEktefelleSamboer> ikkeOmfattetEktefelleSamboers = Set.of(new IkkeOmfattetEktefelleSamboer("uuid4", "SAMBOER_UTEN_FELLES_BARN", "fritekstForUuid4"));
        AvklarteMedfolgendeEktefelleSamboer avklarteMedfolgendeEktefelleSamboer = new AvklarteMedfolgendeEktefelleSamboer(omfattetEktefelleSamboer, ikkeOmfattetEktefelleSamboers);

        MedfolgendeFamilieDto medfolgendeFamilieDto = new MedfolgendeFamilieDto(avklarteMedfolgendeBarnFtrl, avklarteMedfolgendeEktefelleSamboer);

        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(Set.of(
            lagAvklartefaktaDto("uuid1", Avklartefaktatyper.VURDERING_LOVVALG_BARN, true, "", ""),
            lagAvklartefaktaDto("uuid2", Avklartefaktatyper.VURDERING_LOVVALG_BARN, false, "fritekstForUuid2", "OVER_18_AR"),
            lagAvklartefaktaDto("uuid3", Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, true, "", ""),
            lagAvklartefaktaDto("uuid4", Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, false, "fritekstForUuid4", "SAMBOER_UTEN_FELLES_BARN")));

        AvklartefaktaOppsummeringDto response = avklartefaktaTjeneste.lagreMedfolgendeFamilieSomAvklarteFakta(1L, medfolgendeFamilieDto);

        Set<OmfattetFamilie> responseOmfattetBarn = response.getMedfolgendeFamilie().getAvklarteMedfolgendeBarnFtrl().barnOmfattetAvNorskTrygd;
        assertEquals(responseOmfattetBarn.size(), 1);
        assertEquals(responseOmfattetBarn.iterator().next().uuid, omfattetBarn.iterator().next().uuid);

        Set<IkkeOmfattetBarnFtrl> responseIkkeOmfattetBarn = response.getMedfolgendeFamilie().getAvklarteMedfolgendeBarnFtrl().barnIkkeOmfattetAvNorskTrygd;
        assertEquals(responseIkkeOmfattetBarn.size(), 1);
        assertEquals(responseIkkeOmfattetBarn.iterator().next().uuid, ikkeOmfattetBarnFtrls.iterator().next().uuid);
        assertEquals(responseIkkeOmfattetBarn.iterator().next().begrunnelse, ikkeOmfattetBarnFtrls.iterator().next().begrunnelse);
        assertEquals(responseIkkeOmfattetBarn.iterator().next().begrunnelseFritekst, ikkeOmfattetBarnFtrls.iterator().next().begrunnelseFritekst);

        Set<OmfattetFamilie> responseOmfattetEktefelleSamboer = response.getMedfolgendeFamilie().getAvklarteMedfolgendeEktefelleSamboer().ektefelleSamboerOmfattetAvNorskTrygd;
        assertEquals(responseOmfattetEktefelleSamboer.size(), 1);
        assertEquals(responseOmfattetEktefelleSamboer.iterator().next().uuid, omfattetEktefelleSamboer.iterator().next().uuid);

        Set<IkkeOmfattetEktefelleSamboer> responseIkkeOmfattetEktefelleSamboer = response.getMedfolgendeFamilie().getAvklarteMedfolgendeEktefelleSamboer().ektefelleSamboerIkkeOmfattetAvNorskTrygd;
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

