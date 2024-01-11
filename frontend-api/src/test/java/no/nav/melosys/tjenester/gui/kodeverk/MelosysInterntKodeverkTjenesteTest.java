package no.nav.melosys.tjenester.gui.kodeverk;

import java.util.Collection;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.domain.kodeverk.Trygdedekninger.*;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {MelosysInterntKodeverkTjeneste.class})
class MelosysInterntKodeverkTjenesteTest {

    @MockBean
    private MedlemskapsperiodeService medlemskapsperiodeService;

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/kodeverk/melosys-internt";

    private static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER = Set.of(FTRL_2_9_FØRSTE_LEDD_A_HELSE, FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        FTRL_2_9_FØRSTE_LEDD_B_PENSJON, FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON, FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER);

    @Test
    void hentKoderTilFolketrygden() throws Exception {
        when(medlemskapsperiodeService.hentGyldigeTrygdedekninger()).thenReturn(GYLDIGE_TRYGDEDEKNINGER);

        mockMvc.perform(get(BASE_URL + "/folketrygden")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.InnvilgelsesResultat.length()", equalTo(3)))
            .andReturn();

        verify(medlemskapsperiodeService).hentGyldigeTrygdedekninger();
    }
}

