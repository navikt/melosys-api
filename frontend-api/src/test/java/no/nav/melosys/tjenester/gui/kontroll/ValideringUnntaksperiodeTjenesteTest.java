package no.nav.melosys.tjenester.gui.kontroll;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ValideringUnntaksperiodeTjeneste.class)
class ValideringUnntaksperiodeTjenesteTest {


    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private UnntaksperiodeKontrollService unntaksperiodeKontrollService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/kontroll";

    @Test
    void validerUnntak_ok() throws Exception {
        ValideringUnntaksperiodeTjeneste.UnntaksperiodeRequestDto requestDto = new ValideringUnntaksperiodeTjeneste.UnntaksperiodeRequestDto(LocalDate.parse("2020-01-01"), LocalDate.parse("2021-05-15"));

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/unntaksperiode", 22L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
            )
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriser(22L, Aksesstype.LES);
        verify(unntaksperiodeKontrollService).kontrollPeriode(22L, requestDto.tilPeriode());
    }
}
