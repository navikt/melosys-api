package no.nav.melosys.tjenester.gui.avklartefakta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.avgift.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagServiceDeprecated;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.TrygdeavgiftTjeneste;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TrygdeavgiftTjeneste.class})
public class TrygdeavgiftTjenesteTest {

    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private TrygdeavgiftsgrunnlagServiceDeprecated trygdeavgiftsgrunnlagServiceDeprecated;
    @MockBean
    private TrygdeavgiftsberegningService trygdeavgiftsberegningService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/behandlinger/{behandlingID}/trygdeavgift";
    private static final long BEHANDLINGSRESULTAT_ID = 1;
    private static final TrygdeavgiftsgrunnlagDeprecated trygdeavgiftsgrunnlag = lagTrygdeavgiftsgrunnlag();
    private static final Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = lagTrygdeavgiftsberegningresultat();

    @Test
    void oppdaterAvgiftsgrunnlag() throws Exception {
        var dto = new OppdaterAvgiftsgrunnlagDto(
            Loenn_forhold.DELT_LØNN,
            new AvgiftsgrunnlagInfoDto(true, true, null),
            new AvgiftsgrunnlagInfoDto(true, true, null)
        );
        when(trygdeavgiftsgrunnlagServiceDeprecated.oppdaterAvgiftsgrunnlag(eq(BEHANDLINGSRESULTAT_ID), any()))
            .thenReturn(trygdeavgiftsgrunnlag);

        mockMvc.perform(put(BASE_URL + "/grunnlag", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(AvgiftsgrunnlagDto.av(trygdeavgiftsgrunnlag), AvgiftsgrunnlagDto.class));
    }

    @Test
    void hentAvgiftsgrunnlag() throws Exception {
        when(trygdeavgiftsgrunnlagServiceDeprecated.hentAvgiftsgrunnlag(eq(BEHANDLINGSRESULTAT_ID)))
            .thenReturn(trygdeavgiftsgrunnlag);

        mockMvc.perform(get(BASE_URL + "/grunnlag", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(AvgiftsgrunnlagDto.av(trygdeavgiftsgrunnlag), AvgiftsgrunnlagDto.class));
    }

    @Test
    void oppdaterBeregningsgrunnlag() throws Exception {
        var dto = new OppdaterBeregningsgrunnlagDto(100L, null);
        when(trygdeavgiftsberegningService.hentBeregningsresultat(eq(BEHANDLINGSRESULTAT_ID)))
            .thenReturn(lagTrygdeavgiftsberegningresultat());

        mockMvc.perform(put(BASE_URL + "/beregning", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(BeregningsresultatDto.av(trygdeavgiftsberegningsresultat), BeregningsresultatDto.class));
    }

    @Test
    void hentBeregningsresultat() throws Exception {
        when(trygdeavgiftsberegningService.hentBeregningsresultat(eq(BEHANDLINGSRESULTAT_ID)))
            .thenReturn(lagTrygdeavgiftsberegningresultat());

        mockMvc.perform(get(BASE_URL + "/beregning", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(BeregningsresultatDto.av(trygdeavgiftsberegningsresultat), BeregningsresultatDto.class));
    }

    private static Trygdeavgiftsberegningsresultat lagTrygdeavgiftsberegningresultat() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        return new Trygdeavgiftsberegningsresultat(
            100L,
            null,
            aktoer,
            Collections.singleton(new Avgiftsperiode(
                LocalDate.now(), LocalDate.now(), Trygdedekninger.HELSEDEL, new BigDecimal("1.1"), new BigDecimal("1.1"), true)
            ));
    }

    private static TrygdeavgiftsgrunnlagDeprecated lagTrygdeavgiftsgrunnlag() {
        return new TrygdeavgiftsgrunnlagDeprecated(
            Loenn_forhold.DELT_LØNN,
            new AvgiftsgrunnlagInfoNorge(true, true, null, NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV),
            new AvgiftsgrunnlagInfoUtland(true, true, null, UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV)
        );
    }

}

