package no.nav.melosys.tjenester.gui;

import java.io.IOException;

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

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtakService, tilgang);
    }

    @Test
    public void fattVedtak() throws FunksjonellException, TekniskException, IOException {
        VedtakDto vedtakDto = new VedtakDto();
        vedtakDto.setBehandlingsresultatType("HENLEGGELSE");
        long behandlingID = 3;
        vedtakTjeneste.fattVedtak(behandlingID, vedtakDto);

        verify(tilgang, times(1)).sjekk(behandlingID);
        verify(vedtakService, times(1)).fattVedtak(behandlingID, vedtakDto.getBehandlingsresultatType());

        valider(vedtakDto);
    }

}