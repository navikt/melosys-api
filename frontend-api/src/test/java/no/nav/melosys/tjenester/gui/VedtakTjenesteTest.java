package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import javax.ws.rs.BadRequestException;

import no.nav.melosys.domain.BehandlingsresultatType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.tjenester.gui.dto.VedtakDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VedtakTjenesteTest extends JsonSchemaTest {

    @Mock
    private VedtakService vedtakService;

    @Mock
    private Tilgang tilgang;

    private VedtakTjeneste vedtakTjeneste;

    private static final String schemaType = "vedtak-post-schema.json";

    private VedtakDto vedtakDto;

    private long behandlingID;

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtakService, tilgang);
        vedtakDto = new VedtakDto();
        behandlingID = 3;
    }

    @Test
    public void fattVedtak_henleggelse_fungerer() throws FunksjonellException, TekniskException, IOException {
        vedtakDto.setBehandlingsresultattype(BehandlingsresultatType.HENLEGGELSE);
        vedtakTjeneste.fattVedtak(behandlingID, vedtakDto);

        verify(tilgang, times(1)).sjekk(behandlingID);
        verify(vedtakService, times(1)).fattVedtak(behandlingID, vedtakDto.getBehandlingsresultattype());

        valider(vedtakDto);
    }

    @Test
    public void fattVedtak_anmodningOmUnntak_fungerer() throws FunksjonellException, TekniskException, IOException {
        vedtakDto.setBehandlingsresultattype(BehandlingsresultatType.ANMODNING_OM_UNNTAK);
        vedtakTjeneste.fattVedtak(behandlingID, vedtakDto);

        verify(tilgang, times(1)).sjekk(behandlingID);
        verify(vedtakService, times(1)).anmodningOmUnntak(behandlingID);

        valider(vedtakDto);
    }

    @Test(expected = BadRequestException.class)
    public void fattVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException, IOException {
        vedtakTjeneste.fattVedtak(behandlingID, vedtakDto);

        verify(tilgang, times(1)).sjekk(behandlingID);
        valider(vedtakDto);
    }
}