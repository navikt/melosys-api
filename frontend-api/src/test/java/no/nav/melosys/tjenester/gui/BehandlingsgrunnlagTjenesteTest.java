package no.nav.melosys.tjenester.gui;


import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BehandlingsgrunnlagTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(BehandlingsgrunnlagTjenesteTest.class);

    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private BehandlingsgrunnlagTjeneste behandlingsgrunnlagTjeneste;

    @Before
    public void setup() {
        behandlingsgrunnlagTjeneste = new BehandlingsgrunnlagTjeneste(behandlingsgrunnlagService);
    }

    @Test
    public void hentBehandlingsgrunnlag() {
        //TODO: schema-validering
        //valider(behandlingsgrunnlagDataJson, "soknader-schema.json", log);
    }
}