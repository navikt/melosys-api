package no.nav.melosys.tjenester.gui.avklartefakta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.Penger;
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge;
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Skatteplikttype;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.TrygdeavgiftTjeneste;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftsgrunnlagDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TrygdeavgiftTjeneste.class})
class TrygdeavgiftTjenesteTest {

    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private TrygdeavgiftsberegningService trygdeavgiftsberegningService;
    @MockBean
    private TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/behandlinger/{behandlingID}/trygdeavgift";
    private static final long BEHANDLINGSRESULTAT_ID = 1;
    private static final Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag = lagTrygdeavgiftsgrunnlag();
    private static final Set<Trygdeavgiftsperiode> trygdeavgiftsperioder = lagTrygdeavgiftsperioder();

    @Test
    void hentTrygdeavgiftsgrunnlag() throws Exception {
        when(trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlag(BEHANDLINGSRESULTAT_ID)).thenReturn(trygdeavgiftsgrunnlag);

        mockMvc.perform(get(BASE_URL + "/grunnlag", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(new TrygdeavgiftsgrunnlagDto(trygdeavgiftsgrunnlag), TrygdeavgiftsgrunnlagDto.class));
    }

    @Test
    void oppdaterTrygdeavgiftsgrunnlag() throws Exception {
        var dto = new TrygdeavgiftsgrunnlagDto(Skatteplikttype.SKATTEPLIKTIG, Collections.emptySet());
        when(trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(eq(BEHANDLINGSRESULTAT_ID), any(OppdaterTrygdeavgiftsgrunnlagRequest.class)))
            .thenReturn(trygdeavgiftsgrunnlag);

        mockMvc.perform(put(BASE_URL + "/grunnlag", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(new TrygdeavgiftsgrunnlagDto(trygdeavgiftsgrunnlag), TrygdeavgiftsgrunnlagDto.class));
    }

    @Test
    void hentTrygdeavgiftsperioder() throws Exception {
        when(trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLINGSRESULTAT_ID)).thenReturn(trygdeavgiftsperioder);

        mockMvc.perform(get(BASE_URL + "/beregning", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(BeregnetTrygdeavgiftDto.av(trygdeavgiftsperioder), BeregnetTrygdeavgiftDto.class));
    }

    @Test
    void beregnTrygdeavgift() throws Exception {
        when(trygdeavgiftsberegningService.beregnTrygdeavgift(BEHANDLINGSRESULTAT_ID)).thenReturn(trygdeavgiftsperioder);

        mockMvc.perform(put(BASE_URL + "/beregning", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(BeregnetTrygdeavgiftDto.av(trygdeavgiftsperioder), BeregnetTrygdeavgiftDto.class));
    }

    private static Trygdeavgiftsgrunnlag lagTrygdeavgiftsgrunnlag() {
        var skatteForholdINorge = new SkatteforholdTilNorge();
        skatteForholdINorge.setSkatteplikttype(Skatteplikttype.SKATTEPLIKTIG);
        var trygdeavgiftsgrunnlag = new Trygdeavgiftsgrunnlag();
        trygdeavgiftsgrunnlag.setSkatteforholdTilNorge(Set.of(skatteForholdINorge));
        trygdeavgiftsgrunnlag.setInntektsperioder(Collections.emptySet());

        return trygdeavgiftsgrunnlag;
    }

    private static Set<Trygdeavgiftsperiode> lagTrygdeavgiftsperioder() {
        var trygdeavgift = new Trygdeavgiftsperiode();
        trygdeavgift.setPeriodeFra(LocalDate.now());
        trygdeavgift.setPeriodeTil(LocalDate.now().plusDays(10));
        trygdeavgift.setTrygdesats(7.9);
        trygdeavgift.setTrygdeavgiftsbeløpMd(new Penger(BigDecimal.valueOf(10000)));

        var medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setFom(LocalDate.now());
        medlemskapsperiode.setTom(LocalDate.now().plusDays(10));
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.HELSEDEL);
        trygdeavgift.setFastsattTrygdeavgift(new FastsattTrygdeavgift());
        trygdeavgift.getFastsattTrygdeavgift().setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        trygdeavgift.getFastsattTrygdeavgift().getMedlemAvFolketrygden().setMedlemskapsperioder(Set.of(medlemskapsperiode));

        return Set.of(trygdeavgift);
    }
}

