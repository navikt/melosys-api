package no.nav.melosys.tjenester.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.behandling.VilkaarsresultatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {VilkaarTjeneste.class})
public class VilkaarTjenesteTest {

    @MockBean
    private VilkaarsresultatService vilkaarsresultatService;
    @MockBean
    private InngangsvilkaarService inngangsvilkaarService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/vilkaar";

    @Test
    void hentVilkår() throws Exception {
        when(vilkaarsresultatService.hentVilkaar(anyLong())).thenReturn(lagVilkaarDTOList());

        mockMvc.perform(get(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].vilkaar", equalTo(Vilkaar.FO_883_2004_ART12_1.getKode())));
    }

    @Test
    void registrerVilkår() throws Exception {
        var dto = lagVilkaarDTOList();
        when(vilkaarsresultatService.hentVilkaar(anyLong())).thenReturn(lagVilkaarDTOList());

        mockMvc.perform(post(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].vilkaar", equalTo(Vilkaar.FO_883_2004_ART12_1.getKode())));
    }

    @Test
    void overstyrInngangsvilkårTilOppfylt() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{behandlingID}/inngangsvilkaar/overstyr", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
    }

    private static List<VilkaarDto> lagVilkaarDTOList() {
        var dto = new VilkaarDto();
        dto.setVilkaar(Vilkaar.FO_883_2004_ART12_1.getKode());
        Set<String> koder = new HashSet<>();
        koder.add(Art12_1_begrunnelser.ERSTATTER_ANNEN.getKode());
        dto.setBegrunnelseKoder(koder);
        return List.of(dto);
    }
}
