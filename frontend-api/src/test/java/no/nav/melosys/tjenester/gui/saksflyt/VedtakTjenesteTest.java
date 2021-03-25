package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.vedtak.*;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VedtakTjenesteTest extends JsonSchemaTestParent {
    private static final String FATT_VEDTAK_SCHEMA = "saksflyt-vedtak-fatt-post-schema.json";
    private static final String ENDRE_PERIODE_SCHEMA = "saksflyt-vedtak-endre-post-schema.json";
    private static final long behandlingID = 3;

    @Mock
    private VedtakServiceFasade vedtakServiceFasade;
    @Mock
    private TilgangService tilgangService;

    private VedtakTjeneste vedtakTjeneste;

    @Override
    protected ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @BeforeEach
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtakServiceFasade, tilgangService);
    }

    @Test
    void fattVedtakEos_henleggelse_fungerer() throws MelosysException, IOException {
        EosFattVedtakDto fattVedtakDto = new EosFattVedtakDto.Builder()
            .medBehandlingsresultat(Behandlingsresultattyper.HENLEGGELSE)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medMottakerInstitusjoner("SE:4343")
            .build();
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakServiceFasade).fattVedtak(behandlingID, fattVedtakDto);

        //TODO Bytt schema
        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test
    void fattVedtakFtrl_henleggelse_fungerer() throws MelosysException, IOException {
        FtrlFattVedtakDto fattVedtakDto = new FtrlFattVedtakDto.Builder()
            .medBehandlingsresultat(Behandlingsresultattyper.HENLEGGELSE)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medFritekstInnledning("Innledning")
            .build();

        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakServiceFasade).fattVedtak(behandlingID, fattVedtakDto);


        //TODO Bytt schema
//        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test
    void fattVedtak_dtoManglerBehandlingresultat_girException() {
        EosFattVedtakDto fattVedtakDto = new EosFattVedtakDto.Builder()
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .build();

        assertThatThrownBy(() -> vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BehandlingsresultatTypeKode eller vedtakstype mangler.");
    }

    @Test
    void fattVedtak_dtoManglerVedtakstype_girException() {
        FattVedtakDto fattVedtakDto = new EosFattVedtakDto.Builder()
            .medBehandlingsresultat(Behandlingsresultattyper.HENLEGGELSE)
            .build();

        assertThatThrownBy(() -> vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BehandlingsresultatTypeKode eller vedtakstype mangler.");
    }

    @Test
    void endreVedtak_fungerer() throws FunksjonellException, TekniskException, IOException {
        EndreVedtakDto endreVedtakDto = new EndreVedtakDto.Builder()
            .medBegrunnelseKode(Endretperiode.ENDRINGER_ARBEIDSSITUASJON)
            .build();

        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(tilgangService).sjekkTilgang(behandlingID);
        verify(vedtakServiceFasade).endreVedtak(behandlingID, endreVedtakDto);

        valider(endreVedtakDto, ENDRE_PERIODE_SCHEMA);
    }

    @Test
    void endreVedtak_dtoManglerBehandlingresultat_girException() {
        assertThatThrownBy(() -> vedtakTjeneste.endreVedtak(behandlingID, new EndreVedtakDto.Builder().build()))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BegrunnelseKode mangler.");
    }
}