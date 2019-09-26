package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.BucerTilknyttetBehandlingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.OpprettBucSvarDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private static final String MOTTAKERINSTITUSJONER_SCHEMA = "eessi-mottakerinstitusjoner-schema.json";
    private static final String OPPRETT_BUC_SCHEMA = "eessi-bucer-post-schema.json";
    private static final String BUCER_UNDER_ARBEID_SCHEMA = "eessi-bucer-schema.json";

    private static final String MOCK_RINA_URL = "http://rina-url.local/";

    @Mock
    private EessiService eessiService;
    @Mock
    private BehandlingService behandlingService;

    private EessiTjeneste eessiTjeneste;

    @Before
    public void setup() throws IkkeFunnetException {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandling(eq(123L))).thenReturn(behandling);

        eessiTjeneste = new EessiTjeneste(eessiService, behandlingService);
    }

    @Test
    public void hentMottakerInstitusjoner() throws IOException, MelosysException {
        when(eessiService.hentEessiMottakerinstitusjoner(anyString()))
            .thenReturn(Arrays.asList(
                defaultEasyRandom().nextObject(Institusjon.class),
                defaultEasyRandom().nextObject(Institusjon.class),
                defaultEasyRandom().nextObject(Institusjon.class)
            ));

        Response response = eessiTjeneste.hentMottakerinstitusjoner("LA_BUC_01");
        assertThat(response.getEntity()).isInstanceOf(List.class);
        assertThat((List) response.getEntity()).hasOnlyElementsOfType(Institusjon.class);

        List<Institusjon> institusjoner = (List<Institusjon>) response.getEntity();
        assertThat(institusjoner).isNotEmpty();
        validerArray(institusjoner, MOTTAKERINSTITUSJONER_SCHEMA, log);
    }

    @Test
    public void opprettBuc() throws IOException, MelosysException {
        when(eessiService.opprettBucOgSed(any(), any(BucType.class), anyString(), anyString())).thenReturn(MOCK_RINA_URL);

        BucBestillingDto nyBucDto = new BucBestillingDto(BucType.LA_BUC_01, "NAVT002", "NO");
        Response response = eessiTjeneste.opprettBuc(nyBucDto, 123L);
        assertThat(response.getEntity()).isExactlyInstanceOf(OpprettBucSvarDto.class);
        OpprettBucSvarDto opprettBucSvarDto = (OpprettBucSvarDto) response.getEntity();

        valider(nyBucDto, OPPRETT_BUC_SCHEMA, log);
        assertThat(opprettBucSvarDto.getRinaUrl()).isEqualTo(MOCK_RINA_URL);
    }

    @Test
    public void hentBucer() throws IOException, MelosysException {
        when(eessiService.hentTilknyttedeBucer(anyLong(), anyList()))
            .thenReturn(Arrays.asList(
                bucInformasjon(),
                bucInformasjon(),
                bucInformasjon()
            ));

        Response response = eessiTjeneste.hentBucer(123L, Arrays.asList("utkast", "sendt"));
        assertThat(response.getEntity()).isInstanceOf(BucerTilknyttetBehandlingDto.class);

        BucerTilknyttetBehandlingDto dto = (BucerTilknyttetBehandlingDto) response.getEntity();
        assertThat(dto.getBucer()).hasOnlyElementsOfType(BucInformasjon.class);

        valider(dto, BUCER_UNDER_ARBEID_SCHEMA, log);
    }

    private BucInformasjon bucInformasjon() {
        return new BucInformasjon(
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(LocalDate.class),
            Arrays.asList(
                sedInformasjonMedGyldigUrl(),
                sedInformasjonMedGyldigUrl()
            )
        );
    }

    private SedInformasjon sedInformasjonMedGyldigUrl() {
        return new SedInformasjon(
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(LocalDate.class),
            defaultEasyRandom().nextObject(LocalDate.class),
            defaultEasyRandom().nextObject(String.class),
            defaultEasyRandom().nextObject(String.class),
            MOCK_RINA_URL
        );
    }
}