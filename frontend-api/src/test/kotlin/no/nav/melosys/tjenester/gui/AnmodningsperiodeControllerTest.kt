package no.nav.melosys.tjenester.gui;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodePostDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AnmodningsperiodeController.class})
class AnmodningsperiodeControllerTest {

    @MockBean
    private AnmodningsperiodeService anmodningsperiodeService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private final EasyRandom random = new EasyRandom(new EasyRandomParameters()
        .excludeField(ofType(Behandlingsresultat.class))
        .randomize(ofType(LovvalgBestemmelse.class), () -> new EnumRandomizer<>(Lovvalgbestemmelser_883_2004.class).getRandomValue()));

    private static final String BASE_URL = "/api/anmodningsperioder";

    @Test
    void hentAnmodningsperioder() throws Exception {
        when(anmodningsperiodeService.hentAnmodningsperioder(1L)).thenReturn(mockAnmodningsperioder());

        mockMvc.perform(get(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anmodningsperioder.length()", equalTo(3)));
    }

    @Test
    void lagreAnmodningsperioder() throws Exception {
        Set<Anmodningsperiode> mockAnmodninger = random.objects(Anmodningsperiode.class, 3).collect(Collectors.toSet());
        when(anmodningsperiodeService.lagreAnmodningsperioder(anyLong(), anyCollection()))
            .thenReturn(mockAnmodninger);
        var postDto = AnmodningsperiodePostDto.av(mockAnmodninger);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.anmodningsperioder.length()", equalTo(3)));

        verify(aksesskontroll).autoriserSkriv(anyLong());
        verify(anmodningsperiodeService).lagreAnmodningsperioder(anyLong(), anyCollection());
    }

    @Test
    void hentAnmodningsperiodeSvar() throws Exception {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst("test");
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        anmodningsperiode.setAnmodningsperiodeSvar(anmodningsperiodeSvar);

        when(anmodningsperiodeService.finnAnmodningsperiode(anyLong())).thenReturn(Optional.of(anmodningsperiode));

        mockMvc.perform(get(BASE_URL + "/{anmodningsperiodeID}/svar", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.begrunnelseFritekst", equalTo("test")))
            .andExpect(jsonPath("$.anmodningsperiodeSvarType", equalTo(Anmodningsperiodesvartyper.INNVILGELSE.name())));
    }

    @Test
    void lagreAnmodningsperiodeSvar() throws Exception {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);
        anmodningsperiode.setBehandlingsresultat(behandlingsresultat);

        AnmodningsperiodeSvar svar = new AnmodningsperiodeSvar();
        svar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        svar.setBegrunnelseFritekst("fritekst");
        svar.setAnmodningsperiode(anmodningsperiode);
        anmodningsperiode.setAnmodningsperiodeSvar(svar);

        when(anmodningsperiodeService.finnAnmodningsperiode(anyLong())).thenReturn(Optional.of(anmodningsperiode));
        when(anmodningsperiodeService.lagreAnmodningsperiodeSvarMedLovvalgsperiode(anyLong(), any()))
            .thenReturn(svar);

        mockMvc.perform(post(BASE_URL + "/{anmodningsperiodeID}/svar", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(AnmodningsperiodeSvarDto.tom())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.begrunnelseFritekst", equalTo("fritekst")))
            .andExpect(jsonPath("$.anmodningsperiodeSvarType", equalTo(Anmodningsperiodesvartyper.INNVILGELSE.name())));

        verify(aksesskontroll).autoriserSkriv(anyLong());
    }

    private Set<Anmodningsperiode> mockAnmodningsperioder() {
        return random.objects(Anmodningsperiode.class, 3).collect(Collectors.toSet());
    }
}
