package no.nav.melosys.tjenester.gui.saksflyt;

import java.time.LocalDate;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.dto.GodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.IkkeGodkjennUnntaksperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UnntakTjeneste.class})
class UnntakTjenesteTest {

    @MockBean
    private UnntaksperiodeService unntaksperiodeService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/saksflyt/unntaksperioder";

    @Test
    public void godkjennUnntaksPeriode_godkjennerPeriode() throws Exception {
        var periodeDto = new PeriodeDto(LocalDate.of(2001,1,1),LocalDate.of(2002, 1,1));
        var dto = new GodkjennUnntaksperiodeDto(true, "tekst", periodeDto, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1.toString());

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/godkjenn", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());
    }

    @Test
    public void ikkeGodkjennUnntaksPeriode() throws Exception {
        IkkeGodkjennUnntaksperiodeDto dto = new IkkeGodkjennUnntaksperiodeDto(Collections.emptySet(), "fritekst");

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/ikkegodkjenn", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());
    }
}
