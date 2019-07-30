package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;
import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VedtakTjenesteTest extends JsonSchemaTestParent {
    private static final String FATT_VEDTAK_SCHEMA = "saksflyt-vedtak-post-schema.json";
    private static final String ENDRE_PERIODE_SCHEMA = "saksflyt-vedtak-endre-periode-schema.json";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private VedtakService vedtakService;
    @Mock
    private TilgangService tilgangService;

    private VedtakTjeneste vedtakTjeneste;
    private FattVedtakDto fattVedtakDto;
    private EndreVedtakDto endreVedtakDto;

    private long behandlingID;
    private String schemaType;

    @Override
    protected ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtakService, tilgangService);
        fattVedtakDto = new FattVedtakDto();
        endreVedtakDto = new EndreVedtakDto();
        behandlingID = 3;
    }

    @Test
    public void fattVedtak_henleggelse_fungerer() throws FunksjonellException, TekniskException, IOException {
        schemaType = FATT_VEDTAK_SCHEMA;
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakService).fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultatTypeKode());

        valider(fattVedtakDto);
    }

    @Test(expected = BadRequestException.class)
    public void fattVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException, IOException {
        schemaType = FATT_VEDTAK_SCHEMA;
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        valider(fattVedtakDto);
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException, IOException {
        schemaType = ENDRE_PERIODE_SCHEMA;
        endreVedtakDto.setBegrunnelseKode(Endretperioder.ENDRINGER_ARBEIDSSITUASJON);
        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakService).endreVedtak(behandlingID, Endretperioder.ENDRINGER_ARBEIDSSITUASJON);

        valider(endreVedtakDto);
    }

    @Test
    public void endreVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException, IOException {
        schemaType = ENDRE_PERIODE_SCHEMA;
        expectedException.expect(BadRequestException.class);
        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgangService, never()).sjekkTilgang(behandlingID);
        valider(endreVedtakDto);
    }
}