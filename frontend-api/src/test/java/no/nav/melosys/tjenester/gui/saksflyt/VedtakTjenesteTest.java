package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;
import javax.ws.rs.BadRequestException;

import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
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
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private VedtakService vedtakService;

    @Mock
    private Tilgang tilgang;

    private VedtakTjeneste vedtakTjeneste;

    private String schemaType = "saksflyt-vedtak-post-schema.json";

    private FattVedtakDto fattVedtakDto;
    private EndreVedtakDto endreVedtakDto;

    private long behandlingID;

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtakService, tilgang);
        fattVedtakDto = new FattVedtakDto();
        endreVedtakDto = new EndreVedtakDto();
        behandlingID = 3;
    }

    @Test
    public void fattVedtak_henleggelse_fungerer() throws FunksjonellException, TekniskException, IOException {
        fattVedtakDto.setBehandlingsresultattype(Behandlingsresultattyper.HENLEGGELSE);
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        verify(vedtakService).fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultattype());

        valider(fattVedtakDto);
    }

    @Test
    public void fattVedtak_anmodningOmUnntak_fungerer() throws FunksjonellException, TekniskException, IOException {
        fattVedtakDto.setBehandlingsresultattype(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        verify(vedtakService).anmodningOmUnntak(behandlingID);

        valider(fattVedtakDto);
    }

    @Test(expected = BadRequestException.class)
    public void fattVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException, IOException {
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        valider(fattVedtakDto);
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException, IOException {
        endreVedtakDto.setBegrunnelseKode(Endretperioder.ENDRINGER_ARBEIDSSITUASJON);
        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgang).sjekk(behandlingID);
        verify(vedtakService).endreVedtak(behandlingID, Endretperioder.ENDRINGER_ARBEIDSSITUASJON);

        valider(fattVedtakDto);
    }

    @Test
    public void endreVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException, IOException {
        expectedException.expect(BadRequestException.class);
        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgang, never()).sjekk(behandlingID);
        valider(fattVedtakDto);
    }
}