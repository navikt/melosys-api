package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.integrasjon.eessi.dto.InstitusjonDto;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SedinfoDto;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.sed.SedService;
import no.nav.melosys.tjenester.gui.dto.sed.NyBucDto;
import no.nav.melosys.tjenester.gui.dto.sed.SedUnderArbeidDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SedTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(OppgaveTjenesteTest.class);

    private static final String MOTTAKERINSTITUSJONER_SCHEMA = "mottakerinstitusjoner-schema.json";
    private static final String OPPRETT_BUC_SCHEMA = "opprettbuc-post-schema.json";
    private static final String SED_UNDER_ARBEID_SCHEMA = "sedunderarbeid-schema.json";

    private static final String MOCK_RINA_URL = "http://rina-url.local/";

    private String schemaType;

    @Mock
    private SedService sedService;

    @Mock
    private BehandlingRepository behandlingRepository;

    @InjectMocks
    private SedTjeneste sedTjeneste;

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setup() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        behandling.setFagsak(fagsak);

        when(behandlingRepository.findWithSaksopplysningerById(eq(123L))).thenReturn(behandling);
    }

    @Test
    public void hentMottakerInstitusjoner() throws IOException {
        when(sedService.hentMottakerinstitusjoner(anyString()))
            .thenReturn(Arrays.asList(
                defaultEnhancedRandom().nextObject(InstitusjonDto.class),
                defaultEnhancedRandom().nextObject(InstitusjonDto.class),
                defaultEnhancedRandom().nextObject(InstitusjonDto.class)
            ));

        Response response = sedTjeneste.hentMottakerinstitusjoner("LA_BUC_01");
        assertThat(response.getEntity()).isInstanceOf(List.class);
        assertThat(((List) response.getEntity()).iterator().next()).isExactlyInstanceOf(InstitusjonDto.class);

        List<InstitusjonDto> institusjoner = (List<InstitusjonDto>) response.getEntity();
        assertThat(institusjoner).isNotEmpty();
        schemaType = MOTTAKERINSTITUSJONER_SCHEMA;
        validerListe(institusjoner, log);
    }

    @Test
    public void opprettBuc() throws IOException {
        OpprettSedDto svar = new OpprettSedDto();
        svar.setBucId("1234");
        svar.setRinaUrl(MOCK_RINA_URL);
        when(sedService.opprettBucOgSed(any(), anyString(), anyString(), anyString())).thenReturn(svar);

        NyBucDto nyBucDto = new NyBucDto("LA_BUC_01", "NAVT002", "NO");
        Response response = sedTjeneste.opprettBuc(nyBucDto, 123L);
        assertThat(response.getEntity()).isExactlyInstanceOf(String.class);
        String rinaUrl = (String) response.getEntity();

        schemaType = OPPRETT_BUC_SCHEMA;
        valider(nyBucDto, log);
        assertThat(rinaUrl).isEqualTo("\"" + MOCK_RINA_URL + "\"");
    }

    @Test
    public void hentSederUnderArbeid() throws IOException {
        when(sedService.hentTilknyttedeSeder(anyLong()))
            .thenReturn(Arrays.asList(
                sedinfoDtoMedGyldigUrl(),
                sedinfoDtoMedGyldigUrl(),
                sedinfoDtoMedGyldigUrl()
            ));

        Response response = sedTjeneste.hentSederUnderArbeid(123L);
        assertThat(response.getEntity()).isInstanceOf(List.class);
        assertThat(((List) response.getEntity()).iterator().next()).isExactlyInstanceOf(SedUnderArbeidDto.class);

        List<SedinfoDto> sederUnderArbeid = (List<SedinfoDto>) response.getEntity();

        schemaType = SED_UNDER_ARBEID_SCHEMA;
        validerListe(sederUnderArbeid, log);
    }

    private SedinfoDto sedinfoDtoMedGyldigUrl() {
        SedinfoDto sedinfoDto = defaultEnhancedRandom().nextObject(SedinfoDto.class);
        sedinfoDto.setRinaUrl(MOCK_RINA_URL);
        return sedinfoDto;
    }
}