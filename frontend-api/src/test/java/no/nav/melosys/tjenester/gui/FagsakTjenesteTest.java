package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FagsakTjenesteTest extends JsonSchemaTest {

    private FagsakTjeneste tjeneste;

    @Before
    public void setUp() {
        FagsakService fagsakService = mock(FagsakService.class);
        tjeneste = new FagsakTjeneste(fagsakService);

        EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .collectionSizeRange(1, 10).build();
        Fagsak fagsak = random.nextObject(Fagsak.class);
        when(fagsakService.hentFagsak(any())).thenReturn(fagsak);
    }

    @Override
    public String schemaNavn() {
        return "fagsaker-schema.json";
    }

    @Test
    public void hentFagsak() throws IOException, ProcessingException {
        Response response = tjeneste.hentFagsak("TEST");
        FagsakDto fagsakDto = (FagsakDto) response.getEntity();
        JsonNode testNode = new ObjectMapper().valueToTree(fagsakDto);

        ProcessingReport report = hentSchema().validate(testNode);
        assertThat(report.isSuccess());
    }
}