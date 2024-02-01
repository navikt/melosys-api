package no.nav.melosys.tjenester.gui.saksflyt;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakDto;
import no.nav.melosys.tjenester.gui.dto.saksflyt.anmodningunntak.AnmodningUnntakSvarDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AnmodningUnntakTjeneste.class})
class AnmodningUnntakTjenesteTest {

    @MockBean
    private AnmodningUnntakService anmodningUnntakService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/saksflyt/anmodningsperioder";
    private static final long BEHANDLING_ID = 3;

    @Test
    void anmodningOmUnntak() throws Exception {

        final String mottakerInstitusjon = "SE:321";
        final String fritekstSed = "hei hei";

        var anmodningUnntakDto = new AnmodningUnntakDto();
        anmodningUnntakDto.setMottakerinstitusjon(mottakerInstitusjon);
        anmodningUnntakDto.setFritekstSed(fritekstSed);
        final var vedleggDto = new VedleggDto("jpID", "dokID");
        anmodningUnntakDto.setVedlegg(Set.of(vedleggDto));

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/bestill", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anmodningUnntakDto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSkriv(BEHANDLING_ID);
        verify(anmodningUnntakService).anmodningOmUnntak(BEHANDLING_ID, mottakerInstitusjon,
            Set.of(new DokumentReferanse(vedleggDto.journalpostID, vedleggDto.dokumentID)), fritekstSed);

    }

    @Test
    void svar() throws Exception {
        var anmodningUnntakSvarDto = new AnmodningUnntakSvarDto("test");

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/svar", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anmodningUnntakSvarDto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSkriv(BEHANDLING_ID);
        verify(anmodningUnntakService).anmodningOmUnntakSvar(BEHANDLING_ID, "test");
    }
}
