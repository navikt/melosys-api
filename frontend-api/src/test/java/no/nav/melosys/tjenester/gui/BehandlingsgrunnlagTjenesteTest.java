package no.nav.melosys.tjenester.gui;


import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.domain.kodeverk.Flyvningstyper;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag.PeriodeOgLandPostDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.randomizers.misc.EnumRandomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BehandlingsgrunnlagTjeneste.class})
public class BehandlingsgrunnlagTjenesteTest {

    @MockBean
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/behandlingsgrunnlag";
    private EasyRandom random;

    @BeforeEach
    public void setup() {
        random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .randomize(GeografiskAdresse.class, () -> new EasyRandom().nextObject(SemistrukturertAdresse.class))
            .stringLengthRange(2, 10)
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnr").and(ofType(String.class)), new NumericStringRandomizer(9))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9))
            .randomize(named("typeFlyvninger"), () -> new EnumRandomizer<>(Flyvningstyper.class).getRandomValue())
            .randomize(named("uuid"), () -> UUID.randomUUID().toString()));
    }

    @Test
    void hentBehandlingsgrunnlag() throws Exception {
        Soeknad soeknad = random.nextObject(Soeknad.class);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknad);
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(anyLong())).thenReturn(behandlingsgrunnlag);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(behandlingsgrunnlagService).hentBehandlingsgrunnlag(anyLong());
    }

    @Test
    void oppdaterBehandlingsgrunnlag() throws Exception {
        Soeknad soeknad = random.nextObject(Soeknad.class);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setType(Behandlingsgrunnlagtyper.SØKNAD_A1_YRKESAKTIVE_EØS);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(soeknad);
        when(behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(anyLong(), any())).thenReturn(behandlingsgrunnlag);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soeknad)))
            .andExpect(status().isOk());

        verify(behandlingsgrunnlagService).oppdaterBehandlingsgrunnlag(anyLong(), any());
    }

    @Test
    void oppdaterBehandlingsgrunnlagPeriodeOgLand() throws Exception {
        var periodeOgLandPostDto = new PeriodeOgLandPostDto(LocalDate.now(), LocalDate.now().plusYears(1), List.of("Denmark", "Sweden"));

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/periodeOgLand", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(periodeOgLandPostDto)))
            .andExpect(status().isNoContent());

        verify(behandlingsgrunnlagService).oppdaterBehandlingsgrunnlagPeriodeOgLand(anyLong(), any(), any());
    }

}
