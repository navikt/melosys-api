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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {MelosysInterntKodeverkTjeneste.class})
public class MelosysInterntKodeverkTjenesteTest {

    @MockBean
    private MedlemskapsperiodeService medlemskapsperiodeService;

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/kodeverk/melosys-internt";

    private static final Collection<Trygdedekninger> GYLDIGE_TRYGDEDEKNINGER = Set.of(HELSEDEL, HELSEDEL_MED_SYKE_OG_FORELDREPENGER,
        PENSJONSDEL, HELSE_OG_PENSJONSDEL, HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER);

    @Test
    void hentKoderTilFolketrygden() throws Exception {
        when(medlemskapsperiodeService.hentGyldigeTrygdedekninger()).thenReturn(GYLDIGE_TRYGDEDEKNINGER);

        mockMvc.perform(get(BASE_URL + "/folketrygden")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        verify(medlemskapsperiodeService).hentGyldigeTrygdedekninger();
    }
}

