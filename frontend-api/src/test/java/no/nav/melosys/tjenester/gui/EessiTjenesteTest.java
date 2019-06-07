package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.Sedinformasjon;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.dokument.sed.SedService;
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto;
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
public class EessiTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(OppgaveTjenesteTest.class);

    private static final String MOTTAKERINSTITUSJONER_SCHEMA = "mottakerinstitusjoner-schema.json";
    private static final String OPPRETT_BUC_SCHEMA = "opprettbuc-post-schema.json";
    private static final String SED_UNDER_ARBEID_SCHEMA = "sedunderarbeid-schema.json";

    private static final String MOCK_RINA_URL = "http://rina-url.local/";

    private String schemaType;

    @Mock
    private SedService sedService;

    @Mock
    private BehandlingService behandlingService;

    @InjectMocks
    private EessiTjeneste eessiTjeneste;

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setup() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandling(eq(123L))).thenReturn(behandling);
    }

    @Test
    public void hentMottakerInstitusjoner() throws IOException, MelosysException {
        when(sedService.hentMottakerinstitusjoner(anyString()))
            .thenReturn(Arrays.asList(
                defaultEnhancedRandom().nextObject(Institusjon.class),
                defaultEnhancedRandom().nextObject(Institusjon.class),
                defaultEnhancedRandom().nextObject(Institusjon.class)
            ));

        Response response = eessiTjeneste.hentMottakerinstitusjoner("LA_BUC_01");
        assertThat(response.getEntity()).isInstanceOf(List.class);
        assertThat((List) response.getEntity()).hasOnlyElementsOfType(Institusjon.class);

        List<Institusjon> institusjoner = (List<Institusjon>) response.getEntity();
        assertThat(institusjoner).isNotEmpty();
        schemaType = MOTTAKERINSTITUSJONER_SCHEMA;
        validerListe(institusjoner, log);
    }

    @Test
    public void opprettBuc() throws IOException, MelosysException {
        when(sedService.opprettBucOgSed(any(), anyString(), anyString(), anyString())).thenReturn(MOCK_RINA_URL);

        BucBestillingDto nyBucDto = new BucBestillingDto("LA_BUC_01", "NAVT002", "NO");
        Response response = eessiTjeneste.opprettBuc(nyBucDto, 123L);
        assertThat(response.getEntity()).isExactlyInstanceOf(String.class);
        String rinaUrl = (String) response.getEntity();

        schemaType = OPPRETT_BUC_SCHEMA;
        valider(nyBucDto, log);
        assertThat(rinaUrl).isEqualTo("\"" + MOCK_RINA_URL + "\"");
    }

    @Test
    public void hentSederUnderArbeid() throws IOException, MelosysException {
        when(sedService.hentTilknyttedeSeder(anyLong(), anyString()))
            .thenReturn(Arrays.asList(
                sedinformasjonMedGyldigUrl(),
                sedinformasjonMedGyldigUrl(),
                sedinformasjonMedGyldigUrl()
            ));

        Response response = eessiTjeneste.hentSeder(123L, "utkast");
        assertThat(response.getEntity()).isInstanceOf(List.class);
        assertThat((List) response.getEntity()).hasOnlyElementsOfType(Sedinformasjon.class);

        List<Sedinformasjon> sederUnderArbeid = (List<Sedinformasjon>) response.getEntity();

        schemaType = SED_UNDER_ARBEID_SCHEMA;
        validerListe(sederUnderArbeid, log);
    }

    private Sedinformasjon sedinformasjonMedGyldigUrl() {
        Sedinformasjon sedinformasjon = defaultEnhancedRandom().nextObject(Sedinformasjon.class);
        sedinformasjon.setRinaUrl(MOCK_RINA_URL);
        return sedinformasjon;
    }
}