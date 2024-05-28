package no.nav.melosys.tjenester.gui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.Inntektsperiode;
import no.nav.melosys.domain.avgift.Penger;
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge;
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode;
import no.nav.melosys.domain.kodeverk.Inntektskildetype;
import no.nav.melosys.domain.kodeverk.Skatteplikttype;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.behandlinger.trygdeavgift.TrygdeavgiftTjeneste;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.*;
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
    private BehandlingsresultatService behandlingsresultatService;
    @MockBean
    private TrygdeavgiftMottakerService trygdeavgiftMottakerService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/behandlinger/{behandlingID}/trygdeavgift";
    private static final long BEHANDLINGSRESULTAT_ID = 1;
    private static final Set<Trygdeavgiftsperiode> trygdeavgiftsperioder = lagTrygdeavgiftsperioder();

    @Test
    void hentTrygdeavgiftsperioder() throws Exception {
        when(trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLINGSRESULTAT_ID)).thenReturn(trygdeavgiftsperioder);

        mockMvc.perform(get(BASE_URL + "/beregning", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper)
                .containsObjectAsJson(forventetBeregnetTrygdeavgiftDto(), BeregnetTrygdeavgiftDto.class));
    }

    @Test
    void beregnTrygdeavgift() throws Exception {
        TrygdeavgiftsgrunnlagDto trygdeavgiftsgrunnlagDto = lagTrygdeavgiftsgrunnlagDto();
        when(trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(eq(BEHANDLINGSRESULTAT_ID), any())).thenReturn(trygdeavgiftsperioder);

        mockMvc.perform(put(BASE_URL + "/beregning", 1L)
                .content(objectMapper.writeValueAsString(trygdeavgiftsgrunnlagDto))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper)
                .containsObjectAsJson(forventetBeregnetTrygdeavgiftDto(), BeregnetTrygdeavgiftDto.class));
    }

    @Test
    void finnFakturamottaker() throws Exception {
        var MOTTAKER_NAVN = "Fornavn Etternavn";
        when(trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLINGSRESULTAT_ID)).thenReturn(MOTTAKER_NAVN);

        mockMvc.perform(get(BASE_URL + "/fakturamottaker", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper)
                .containsObjectAsJson(new FakturamottakerDto(MOTTAKER_NAVN), FakturamottakerDto.class));
    }

    private BeregnetTrygdeavgiftDto forventetBeregnetTrygdeavgiftDto() {
        return new BeregnetTrygdeavgiftDto(trygdeavgiftsperioder.stream().map(TrygdeavgiftsperiodeDto::new).toList(),
            lagTrygdeavgiftsgrunnlagDto());
    }

    private TrygdeavgiftsgrunnlagDto lagTrygdeavgiftsgrunnlagDto() {
        Set<SkatteforholdTilNorgeDto> skatteforholdTilNorgeDtos = trygdeavgiftsperioder.stream().map(Trygdeavgiftsperiode::getGrunnlagSkatteforholdTilNorge)
            .map(SkatteforholdTilNorgeDto::new).collect(Collectors.toSet());
        Set<InntekskildeDto> inntekskildeDtos = trygdeavgiftsperioder.stream().map(Trygdeavgiftsperiode::getGrunnlagInntekstperiode)
            .map(InntekskildeDto::new).collect(Collectors.toSet());

        return new TrygdeavgiftsgrunnlagDto(skatteforholdTilNorgeDtos, inntekskildeDtos);
    }

    private static Set<Trygdeavgiftsperiode> lagTrygdeavgiftsperioder() {
        var trygdeavgift = new Trygdeavgiftsperiode();
        trygdeavgift.setPeriodeFra(LocalDate.now());
        trygdeavgift.setPeriodeTil(LocalDate.now().plusDays(10));
        trygdeavgift.setTrygdesats(BigDecimal.valueOf(7.9));
        trygdeavgift.setTrygdeavgiftsbeløpMd(new Penger(BigDecimal.valueOf(10000)));

        var medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE);
        trygdeavgift.setGrunnlagMedlemskapsperiode(medlemskapsperiode);

        var inntektsperiode = new Inntektsperiode();
        inntektsperiode.setFomDato(LocalDate.now());
        inntektsperiode.setTomDato(LocalDate.now());
        inntektsperiode.setType(Inntektskildetype.INNTEKT_FRA_UTLANDET);
        trygdeavgift.setGrunnlagInntekstperiode(inntektsperiode);

        var skatteForholdINorge = new SkatteforholdTilNorge();
        skatteForholdINorge.setFomDato(LocalDate.now());
        skatteForholdINorge.setTomDato(LocalDate.now());
        skatteForholdINorge.setSkatteplikttype(Skatteplikttype.SKATTEPLIKTIG);
        trygdeavgift.setGrunnlagSkatteforholdTilNorge(skatteForholdINorge);

        trygdeavgift.setGrunnlagMedlemskapsperiode(medlemskapsperiode);

        return Set.of(trygdeavgift);
    }
}


