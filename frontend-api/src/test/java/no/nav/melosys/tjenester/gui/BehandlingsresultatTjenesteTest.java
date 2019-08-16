package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import javax.ws.rs.core.Response;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Henleggelsesgrunner;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.BehandlingsresultatDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingsresultatTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(BehandlingsresultatTjenesteTest.class);
    private static final String BEHANDLINGSRESULTAT_SCHEMA = "behandlinger-resultat-schema.json";

    private BehandlingsresultatTjeneste behandlingsresultatTjeneste;

    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    @Before
    public void setUp() {
        behandlingsresultatTjeneste = new BehandlingsresultatTjeneste(behandlingsresultatService, mock(TilgangService.class));
    }

    @Test
    public void validerBehandlingsresultat() throws IOException {
        EasyRandomParameters easyRandomParameters = defaultEasyRandomParameters()
            .randomize(named("behandlingsresultatTypeKode").and(ofType(String.class)), () -> new EnumRandomizer<>(Behandlingsresultattyper.class).getRandomValue().getKode());
        BehandlingsresultatDto behandlingsresultat = new EasyRandom(easyRandomParameters).nextObject(BehandlingsresultatDto.class);
        String jsonString = objectMapper().writeValueAsString(behandlingsresultat);
        assertThat(jsonString).isNotEmpty();
        valider(jsonString, BEHANDLINGSRESULTAT_SCHEMA, log);
    }

    @Test
    public void hentBehandlingsresultat_medBehandlingsid_forventerBehandlingsresultatDto() throws FunksjonellException, TekniskException, IOException {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        behandlingsresultat.setBegrunnelseFritekst("Bruker har fått flyskrekk");
        BehandlingsresultatBegrunnelse begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setKode(Henleggelsesgrunner.ANNET.getKode());
        behandlingsresultat.setBehandlingsresultatBegrunnelser(Sets.newHashSet(begrunnelse));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        Response response = behandlingsresultatTjeneste.hentBehandlingsresultat(4L);
        String jsonString = objectMapper().writeValueAsString(response.getEntity());
        assertThat(jsonString).isNotEmpty();
        valider(jsonString, BEHANDLINGSRESULTAT_SCHEMA, log);
    }
}
