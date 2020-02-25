package no.nav.melosys.tjenester.gui;


import java.util.Collections;
import java.util.HashSet;

import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsGrunnlagType;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.BehandlingsgrunnlagGetDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
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

    @Before
    public void setup() throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
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
    }

    @Test
    public void hentBehandlingsgrunnlag_erSoeknad_validerSchema() throws Exception{
        SoeknadDokument soeknadDokument = random.nextObject(SoeknadDokument.class);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(BehandlingsGrunnlagType.SØKNAD);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknadDokument);
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(behandlingsgrunnlag);

        ResponseEntity responseEntity = behandlingsgrunnlagTjeneste.hentBehandlingsgrunnlag(123);
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

        ResponseEntity responseEntity = behandlingsgrunnlagTjeneste.hentBehandlingsgrunnlag(123);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isInstanceOf(BehandlingsgrunnlagGetDto.class);

        String json = objectMapperMedKodeverkServiceStub().writeValueAsString(responseEntity.getBody());
        valider(json, "behandlingsgrunnlag-schema.json", log);
    }
}