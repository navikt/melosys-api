package no.nav.melosys.tjenester.gui.saksflyt;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.melosys.service.vedtak.FattVedtakRequest;
import no.nav.melosys.service.vedtak.VedtaksfattingFasade;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {VedtakTjeneste.class})
class VedtakTjenesteTest {

    @MockBean
    private VedtaksfattingFasade vedtaksfattingFasade;
    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private FerdigbehandlingKontrollService ferdigbehandlingKontrollService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final long BEHANDLING_ID = 3;
    private static final String BASE_URL = "/api/saksflyt/vedtak";

    @Test
    void fattVedtak_henleggelse() throws Exception {
        var dto = new FattVedtakDto();
        dto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        dto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        dto.setMottakerinstitusjoner(Set.of("SE:4343"));

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSkriv(BEHANDLING_ID);
        verify(vedtaksfattingFasade).fattVedtak(eq(BEHANDLING_ID), any(FattVedtakRequest.class));
    }

    @Test
    void fattVedtakFtrl_henleggelse() throws Exception {
        var dto = new FattVedtakDto();
        dto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        dto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        dto.setBegrunnelseFritekst("Begrunnelse");

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSkriv(BEHANDLING_ID);
        verify(vedtaksfattingFasade).fattVedtak(eq(BEHANDLING_ID), any(FattVedtakRequest.class));
    }

    @Test
    void fattVedtakTrygdeavtale_henleggelse_fungerer() throws Exception {
        var dto = new FattVedtakDto();
        dto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        dto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        dto.setBegrunnelseFritekst("Begrunnelse");

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSkriv(BEHANDLING_ID);
        verify(vedtaksfattingFasade).fattVedtak(eq(BEHANDLING_ID), any(FattVedtakRequest.class));
    }

    @Test
    void fattVedtak_dtoManglerBehandlingresultat_girException() throws Exception {
        var dto = new FattVedtakDto();
        dto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("BehandlingsresultatTypeKode eller vedtakstype mangler.")));
    }

    @Test
    void fattVedtak_dtoManglerVedtakstype_girException() throws Exception {
        var dto = new FattVedtakDto();
        dto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/fatt", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("BehandlingsresultatTypeKode eller vedtakstype mangler.")));
    }

    @Test
    void endreVedtak() throws Exception {
        var dto = new EndreVedtakDto();
        dto.setBegrunnelseKode(Endretperiode.ENDRINGER_ARBEIDSSITUASJON);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/endre", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSkriv(BEHANDLING_ID);
        verify(vedtaksfattingFasade).endreVedtak(BEHANDLING_ID, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, null, dto.getFritekstSed());
    }

    @Test
    void endreVedtak_dtoManglerBehandlingresultat_girException() throws Exception{
        mockMvc.perform(post(BASE_URL + "/{behandlingID}/endre", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EndreVedtakDto())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("BegrunnelseKode mangler.")));
    }

    @Test
    void kontrollerVedtak() throws Exception {
        var dto = new FattVedtakDto();
        dto.setBehandlingsresultatTypeKode(Behandlingsresultattyper.HENLEGGELSE);
        dto.setVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK);
        dto.setBegrunnelseFritekst("Begrunnelse");


        mockMvc.perform(post(BASE_URL + "/{behandlingID}/kontroller", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriser(BEHANDLING_ID, Aksesstype.LES);
    }
}
