package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
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
    private static final String FATT_VEDTAK_SCHEMA = "saksflyt-vedtak-fatt-post-schema.json";
    private static final String ENDRE_PERIODE_SCHEMA = "saksflyt-vedtak-endre-post-schema.json";

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

    @Override
    protected ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Before
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtakService, tilgangService);
        fattVedtakDto = new FattVedtakDto();
        endreVedtakDto = new EndreVedtakDto();
        behandlingID = 3;
    }

    @Test
    public void fattVedtak_henleggelse_fungerer() throws MelosysException, IOException {
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        fattVedtakDto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        fattVedtakDto.setMottakerinstitusjoner(Set.of("SE:4343"));
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakService).fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultatTypeKode(), null, null,
            fattVedtakDto.getMottakerinstitusjoner(), fattVedtakDto.getVedtakstype(), null);

        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test(expected = FunksjonellException.class)
    public void fattVedtak_dtoManglerBehandlingresultat_girException() throws MelosysException, IOException {
        fattVedtakDto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test(expected = FunksjonellException.class)
    public void fattVedtak_dtoManglerVedtakstype_girException() throws MelosysException, IOException {
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException, IOException {
        endreVedtakDto.setBegrunnelseKode(Endretperiode.ENDRINGER_ARBEIDSSITUASJON);
        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakService).endreVedtak(behandlingID, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, null, endreVedtakDto.getFritekstSed());

        valider(endreVedtakDto, ENDRE_PERIODE_SCHEMA);
    }

    @Test
    public void endreVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException, IOException {
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("BegrunnelseKode mangler.");
        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgangService, never()).sjekkTilgang(behandlingID);
        valider(endreVedtakDto, ENDRE_PERIODE_SCHEMA);
    }
}