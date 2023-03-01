package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse;
import no.nav.melosys.domain.VedtakMetadata;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.behandling.AngiBehandlingsresultatService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.AngiBehandlingsresultattypeDto;
import no.nav.melosys.tjenester.gui.dto.BehandlingsresultatDto;
import no.nav.melosys.tjenester.gui.dto.LagreFritekstDto;
import no.nav.melosys.tjenester.gui.dto.OppdaterUtfallRegistreringUnntakDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BehandlingsresultatTjeneste.class})
class BehandlingsresultatTjenesteTest {

    @MockBean
    private BehandlingsresultatService behandlingsresultatService;
    @MockBean
    private AngiBehandlingsresultatService angiBehandlingsresultatService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/behandlinger";
    private static final Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();


    @Test
    void hentBehandlingsresultat() throws Exception {
        when(behandlingsresultatService.hentBehandlingsresultatMedKontrollresultat(anyLong())).thenReturn(behandlingsresultat);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}/resultat", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(BehandlingsresultatDto.av(behandlingsresultat), BehandlingsresultatDto.class));
    }

    @Test
    void oppdaterFritekster() throws Exception {
        var dto = new LagreFritekstDto("innledning", "begrunnelse");
        when(behandlingsresultatService.oppdaterFritekster(anyLong(), anyString(), anyString())).thenReturn(behandlingsresultat);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/resultat/fritekst", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(BehandlingsresultatDto.av(behandlingsresultat), BehandlingsresultatDto.class));
    }

    @Test
    void oppdaterUtfallRegistreringUnntak() throws Exception {
        var dto = new OppdaterUtfallRegistreringUnntakDto(Utfallregistreringunntak.DELVIS_GODKJENT);
        when(behandlingsresultatService.oppdaterUtfallRegistreringUnntak(anyLong(), any())).thenReturn(behandlingsresultat);

        mockMvc.perform(put(BASE_URL + "/{behandlingID}/resultat/utfallregistreringunntak", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(BehandlingsresultatDto.av(behandlingsresultat), BehandlingsresultatDto.class));
    }

    @Test
    void angiBehandlingsresultattype() throws Exception {
        var dto = new AngiBehandlingsresultattypeDto(Behandlingsresultattyper.HENLEGGELSE);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/resultat/type", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        behandlingsresultat.setBegrunnelseFritekst("Bruker har fått flyskrekk");
        behandlingsresultat.setInnledningFritekst("<p>Bruker har fått flyskrekk</>");
        var begrunnelse = new BehandlingsresultatBegrunnelse();
        begrunnelse.setKode(Henleggelsesgrunner.ANNET.getKode());
        behandlingsresultat.setBehandlingsresultatBegrunnelser(Sets.newHashSet(begrunnelse));
        var vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setVedtakstype(Vedtakstyper.KORRIGERT_VEDTAK);
        vedtakMetadata.setNyVurderingBakgrunn(Nyvurderingbakgrunner.FEIL_I_BEHANDLING.getKode());
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        return behandlingsresultat;
    }
}
