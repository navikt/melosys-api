package no.nav.melosys.tjenester.gui.saksflyt;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.vedtak.FattVedtakRequest;
import no.nav.melosys.service.vedtak.VedtaksfattingFasade;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.JsonSchemaTestParent;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VedtakTjenesteTest extends JsonSchemaTestParent {
    private static final String FATT_VEDTAK_SCHEMA = "saksflyt-vedtak-fatt-post-schema.json";
    private static final String ENDRE_PERIODE_SCHEMA = "saksflyt-vedtak-endre-post-schema.json";
    private static final long behandlingID = 3;

    @Mock
    private VedtaksfattingFasade vedtaksfattingFasade;
    @Mock
    private Aksesskontroll aksesskontroll;
    @Mock
    private FerdigbehandlingKontrollService ferdigbehandlingKontrollService;

    private VedtakTjeneste vedtakTjeneste;

    @Override
    protected ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @BeforeEach
    public void setUp() {
        vedtakTjeneste = new VedtakTjeneste(vedtaksfattingFasade, aksesskontroll, ferdigbehandlingKontrollService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void fattVedtak_henleggelse_fungerer() throws Exception {
        var fattVedtakDto = new FattVedtakDto();
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        fattVedtakDto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        fattVedtakDto.setMottakerinstitusjoner(Set.of("SE:4343"));
        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(aksesskontroll).autoriserSkriv(behandlingID);
        verify(vedtaksfattingFasade).fattVedtak(eq(behandlingID), any(FattVedtakRequest.class));

        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test
    void fattVedtakFtrl_henleggelse_fungerer() throws Exception {
        FattVedtakDto fattVedtakDto = new FattVedtakDto();
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        fattVedtakDto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        fattVedtakDto.setBegrunnelseFritekst("Begrunnelse");

        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(aksesskontroll).autoriserSkriv(behandlingID);
        verify(vedtaksfattingFasade).fattVedtak(eq(behandlingID), any(FattVedtakRequest.class));

        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test
    void fattVedtakTrygdeavtale_henleggelse_fungerer() throws Exception {
        FattVedtakDto fattVedtakDto = new FattVedtakDto();
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        fattVedtakDto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        fattVedtakDto.setBegrunnelseFritekst("Begrunnelse");

        vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto);

        verify(aksesskontroll).autoriserSkriv(behandlingID);
        verify(vedtaksfattingFasade).fattVedtak(eq(behandlingID), any(FattVedtakRequest.class));

        valider(fattVedtakDto, FATT_VEDTAK_SCHEMA);
    }

    @Test
    void fattVedtak_dtoManglerBehandlingresultat_girException() {
        var fattVedtakDto = new FattVedtakDto();
        fattVedtakDto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);

        assertThatThrownBy(() -> vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BehandlingsresultatTypeKode eller vedtakstype mangler.");
    }

    @Test
    void fattVedtak_dtoManglerVedtakstype_girException() {
        FattVedtakDto fattVedtakDto = new FattVedtakDto();
        fattVedtakDto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);

        assertThatThrownBy(() -> vedtakTjeneste.fattVedtak(behandlingID, fattVedtakDto))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BehandlingsresultatTypeKode eller vedtakstype mangler.");
    }

    @Test
    void endreVedtak_fungerer() throws IOException {
        EndreVedtakDto endreVedtakDto = new EndreVedtakDto();
        endreVedtakDto.setBegrunnelseKode(Endretperiode.ENDRINGER_ARBEIDSSITUASJON);
        vedtakTjeneste.endreVedtak(behandlingID, endreVedtakDto);

        verify(aksesskontroll).autoriserSkriv(behandlingID);
        verify(vedtaksfattingFasade).endreVedtak(behandlingID, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, null, endreVedtakDto.getFritekstSed());

        valider(endreVedtakDto, ENDRE_PERIODE_SCHEMA);
    }

    @Test
    void endreVedtak_dtoManglerBehandlingresultat_girException() {
        assertThatThrownBy(() -> vedtakTjeneste.endreVedtak(behandlingID, new EndreVedtakDto()))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("BegrunnelseKode mangler.");
    }

    @Test
    void skalMappeTilDto_EOS() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        FattVedtakDto eosVedtakDto = objectMapper.readValue(hentJsonRequest("fattEosVedtak.json"), FattVedtakDto.class);

        assertThat(eosVedtakDto)
            .isInstanceOf(FattVedtakDto.class)
            .extracting("mottakerinstitusjoner", "fritekstSed", "nyVurderingBakgrunn")
            .containsExactly(Set.of("NO:NAVT003"), "Fritekst til SED", Nyvurderingbakgrunner.FEIL_I_BEHANDLING.getKode());
    }

    @Test
    void skalMappeTilDto_FTRL() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        FattVedtakDto ftrlVedtakDto = objectMapper.readValue(hentJsonRequest("fattFtrlVedtak.json"), FattVedtakDto.class);

        assertThat(ftrlVedtakDto)
            .isNotNull()
            .isInstanceOf(FattVedtakDto.class)
            .extracting("innledningFritekst", "kopiMottakere", "nyVurderingBakgrunn")
            .containsExactly("Fritekst innledning", null, null);
    }

    @Test
    void skalMappeTilDto_Trygdeavtale() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        FattVedtakDto ftrlVedtakDto = objectMapper.readValue(hentJsonRequest("fattTrygdeavtaleVedtak.json"), FattVedtakDto.class);

        assertThat(ftrlVedtakDto)
            .isNotNull()
            .isInstanceOf(FattVedtakDto.class)
            .extracting("innledningFritekst", "kopiMottakere", "nyVurderingBakgrunn")
            .containsExactly("Fritekst innledning", null, Nyvurderingbakgrunner.NYE_OPPLYSNINGER.getKode());
    }

    private InputStream hentJsonRequest(String filnavn) {
        return getClass().getClassLoader().getResourceAsStream(filnavn);
    }
}
