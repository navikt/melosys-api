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
import no.nav.melosys.service.vedtak.VedtakServiceFasade;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VedtakTjenesteTest extends JsonSchemaTestParent {
    private static final String FATT_VEDTAK_SCHEMA = "saksflyt-vedtak-fatt-post-schema.json";
    private static final String ENDRE_PERIODE_SCHEMA = "saksflyt-vedtak-endre-post-schema.json";

    @Mock
    private VedtakServiceFasade vedtakServiceFasade;
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

    @BeforeEach
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtakServiceFasade, tilgangService);
        fattVedtakDto = new FattVedtakDto();
        endreVedtakDto = new EndreVedtakDto();
        behandlingID = 3;
    }

    @Test
    void fattVedtak_henleggelse_fungerer() throws MelosysException, IOException {
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        fattVedtakDto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        fattVedtakDto.setMottakerinstitusjoner(Set.of("SE:4343"));
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakServiceFasade).fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultatTypeKode(), null, null,
            fattVedtakDto.getMottakerinstitusjoner(), fattVedtakDto.getVedtakstype(), null);

        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test
    void fattVedtak_dtoManglerBehandlingresultat_girException() throws MelosysException {
        fattVedtakDto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);

        assertThatThrownBy(() -> vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BehandlingsresultatTypeKode eller vedtakstype mangler.");

        verify(tilgangService, never()).sjekkTilgang(behandlingID);
    }

    @Test
    void fattVedtak_dtoManglerVedtakstype_girException() throws MelosysException {
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);

        assertThatThrownBy(() -> vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BehandlingsresultatTypeKode eller vedtakstype mangler.");

        verify(tilgangService, never()).sjekkTilgang(behandlingID);
    }

    @Test
    void endreVedtak_fungerer() throws FunksjonellException, TekniskException, IOException {
        endreVedtakDto.setBegrunnelseKode(Endretperiode.ENDRINGER_ARBEIDSSITUASJON);
        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakServiceFasade).endreVedtak(behandlingID, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, null, endreVedtakDto.getFritekstSed());

        valider(endreVedtakDto, ENDRE_PERIODE_SCHEMA);
    }

    @Test
    void endreVedtak_dtoManglerBehandlingresultat_girException() throws FunksjonellException, TekniskException {

        assertThatThrownBy(() -> vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BegrunnelseKode mangler.");

        verify(tilgangService, never()).sjekkTilgang(behandlingID);
    }
}