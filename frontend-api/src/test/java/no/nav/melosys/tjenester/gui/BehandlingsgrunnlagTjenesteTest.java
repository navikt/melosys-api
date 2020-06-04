package no.nav.melosys.tjenester.gui;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.SedGrunnlag;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Overgangsregelbestemmelser;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagGetDto;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagPostDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingsgrunnlagTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(BehandlingsgrunnlagTjenesteTest.class);

    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private RegisterOppslagService registerOppslagService;
    @Mock
    private TilgangService tilgangService;

    private BehandlingsgrunnlagTjeneste behandlingsgrunnlagTjeneste;

    private EasyRandom random;
    private ObjectMapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException, IOException {
        behandlingsgrunnlagTjeneste = new BehandlingsgrunnlagTjeneste(behandlingsgrunnlagService, registerOppslagService, tilgangService);

        random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .randomize(GeografiskAdresse.class, () -> new EasyRandom().nextObject(SemistrukturertAdresse.class))
            .stringLengthRange(2, 10)
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnr").and(ofType(String.class)), new NumericStringRandomizer(9))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9)));

        OrganisasjonDokument organisasjonDokument = random.nextObject(OrganisasjonDokument.class);
        when(registerOppslagService.hentOrganisasjoner(anySet())).thenReturn(new HashSet<>(Collections.singletonList(organisasjonDokument)));

        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void hentBehandlingsgrunnlag_erSoeknad_validerSchema() throws Exception{
        SoeknadDokument soeknadDokument = random.nextObject(SoeknadDokument.class);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.SØKNAD);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknadDokument);
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(behandlingsgrunnlag);

        ResponseEntity<BehandlingsgrunnlagGetDto> responseEntity = behandlingsgrunnlagTjeneste.hentBehandlingsgrunnlag(123);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isInstanceOf(BehandlingsgrunnlagGetDto.class);

        String json = objectMapperMedKodeverkServiceStub().writeValueAsString(responseEntity.getBody());
        valider(json, "behandlingsgrunnlag-schema.json", log);
    }

    @Test
    public void hentBehandlingsgrunnlag_erGenereltBehandlingsgrunnlagData_validerSchema() throws Exception{
        BehandlingsgrunnlagData soeknadDokument = random.nextObject(BehandlingsgrunnlagData.class);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.GENERELT);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknadDokument);
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(behandlingsgrunnlag);

        ResponseEntity<BehandlingsgrunnlagGetDto> responseEntity = behandlingsgrunnlagTjeneste.hentBehandlingsgrunnlag(123);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isInstanceOf(BehandlingsgrunnlagGetDto.class);

        String json = objectMapperMedKodeverkServiceStub().writeValueAsString(responseEntity.getBody());
        valider(json, "behandlingsgrunnlag-schema.json", log);
    }

    @Test
    public void hentBehandlingsgrunnlag_erSedGrunnlag_validerSchema() throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException, IOException {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.SED);

        SedGrunnlag sedGrunnlag = random.nextObject(SedGrunnlag.class);
        sedGrunnlag.overgangsregelbestemmelser = List.of(Overgangsregelbestemmelser.FO_1408_1971_ART14_2_A, Overgangsregelbestemmelser.FO_1408_1971_ART14_2_B);
        sedGrunnlag.ytterligereInformasjon = "fritekst";
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(sedGrunnlag);

        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(behandlingsgrunnlag);

        ResponseEntity<BehandlingsgrunnlagGetDto> responseEntity = behandlingsgrunnlagTjeneste.hentBehandlingsgrunnlag(1);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isInstanceOf(BehandlingsgrunnlagGetDto.class);

        String json = objectMapperMedKodeverkServiceStub().writeValueAsString(responseEntity.getBody());
        valider(json, "behandlingsgrunnlag-schema.json", log);
    }

    @Test
    public void oppdaterBehandlingsgrunnlag_gyldigPayload_validererOK() throws TekniskException, IOException, FunksjonellException {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.SØKNAD);

        BehandlingsgrunnlagData behandlingsgrunnlagData = random.nextObject(BehandlingsgrunnlagData.class);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        when(behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(anyLong(), any())).thenReturn(behandlingsgrunnlag);

        BehandlingsgrunnlagPostDto behandlingsgrunnlagPostDto = new BehandlingsgrunnlagPostDto();
        behandlingsgrunnlagPostDto.setData(mapper.readTree(mapper.writeValueAsString(
            new ImmutablePair<String, BehandlingsgrunnlagData>("data", behandlingsgrunnlagData))));

        behandlingsgrunnlagTjeneste.oppdaterBehandlingsgrunnlag(112L, behandlingsgrunnlagPostDto);
    }

    @Test
    public void oppdaterBehandlingsgrunnlag_ugyldigPayload_kasterException() throws TekniskException, IOException, FunksjonellException {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.SØKNAD);

        BehandlingsgrunnlagData behandlingsgrunnlagData = random.nextObject(BehandlingsgrunnlagData.class);
        behandlingsgrunnlagData.oppholdUtland.oppholdsPeriode = null;
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        BehandlingsgrunnlagPostDto behandlingsgrunnlagPostDto = new BehandlingsgrunnlagPostDto();
        behandlingsgrunnlagPostDto.setData(mapper.readTree(mapper.writeValueAsString(
            new ImmutablePair<String, BehandlingsgrunnlagData>("data", behandlingsgrunnlagData))));

        expectedException.expect(TekniskException.class);
        expectedException.expectMessage("Ugyldig behandlingsgrunnlagData");

        behandlingsgrunnlagTjeneste.oppdaterBehandlingsgrunnlag(112L, behandlingsgrunnlagPostDto);
    }
}