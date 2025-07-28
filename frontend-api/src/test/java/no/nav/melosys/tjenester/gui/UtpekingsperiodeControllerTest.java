package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UtpekingsperiodeController.class})
class UtpekingsperiodeControllerTest {

    private final EasyRandom random = new EasyRandom(new EasyRandomParameters()
        .randomize(ofType(LovvalgBestemmelse.class), () -> new EnumRandomizer<>(Lovvalgbestemmelser_883_2004.class).getRandomValue())
        .randomize(SaksopplysningDokument.class, PersonDokument::new) // for interfaces
    );

    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private UtpekingService utpekingService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/utpekingsperioder";

    @Test
    void hentUtpekingsperioder() throws Exception {
        List<Utpekingsperiode> utpekingsperioder = lagUtpekingsperioder();
        UtpekingsperioderDto utpekingsperioderDto = UtpekingsperioderDto.av(utpekingsperioder);

        when(utpekingService.hentUtpekingsperioder(123L)).thenReturn(utpekingsperioder);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}", 123L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(utpekingsperioderDto, UtpekingsperioderDto.class));

    }

    @Test
    void lagreUtpekingsperioder() throws Exception {
        List<Utpekingsperiode> utpekingsperioder = lagUtpekingsperioder();
        UtpekingsperioderDto utpekingsperioderDto = UtpekingsperioderDto.av(utpekingsperioder);

        when(utpekingService.lagreUtpekingsperioder(eq(123L), any())).thenReturn(utpekingsperioder);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}", 123L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(utpekingsperioderDto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(utpekingsperioderDto, UtpekingsperioderDto.class));

        verify(aksesskontroll).autoriserSkriv(anyLong());
    }

    private List<Utpekingsperiode> lagUtpekingsperioder() {
        Utpekingsperiode utpekingsperiodeUtenTilleggsbestemmelse = new Utpekingsperiode(
            LocalDate.now(),
            LocalDate.now(),
            Land_iso2.SE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
            null
        );

        return Arrays.asList(
            utpekingsperiodeUtenTilleggsbestemmelse,
            random.nextObject(Utpekingsperiode.class),
            random.nextObject(Utpekingsperiode.class)
        );
    }
}
