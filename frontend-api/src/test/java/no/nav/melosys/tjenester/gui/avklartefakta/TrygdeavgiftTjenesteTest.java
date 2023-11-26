package no.nav.melosys.tjenester.gui.avklartefakta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.*;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Inntektskildetype;
import no.nav.melosys.domain.kodeverk.Skatteplikttype;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService;
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.TrygdeavgiftTjeneste;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.FakturamottakerDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftsgrunnlagDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftsperiodeDto;
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
    @MockBean
    private TrygdeavgiftMottakerService trygdeavgiftMottakerService;

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
        when(trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag(BEHANDLINGSRESULTAT_ID)).thenReturn(trygdeavgiftsgrunnlag);

        mockMvc.perform(get(BASE_URL + "/grunnlag", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(new TrygdeavgiftsgrunnlagDto(trygdeavgiftsgrunnlag), TrygdeavgiftsgrunnlagDto.class));
    }

    @Test
    void oppdaterTrygdeavgiftsgrunnlag() throws Exception {
        var dto = new TrygdeavgiftsgrunnlagDto(Collections.emptyList(), Collections.emptyList());
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
            .andExpect(responseBody(objectMapper)
                .containsObjectAsJson(forventetBeregnetTrygdeavgiftDto(), BeregnetTrygdeavgiftDto.class));
    }

    @Test
    void beregnTrygdeavgift() throws Exception {
        when(trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLINGSRESULTAT_ID)).thenReturn(trygdeavgiftsperioder);

        mockMvc.perform(put(BASE_URL + "/beregning", 1L)
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
        return new BeregnetTrygdeavgiftDto(trygdeavgiftsperioder.stream().map(TrygdeavgiftsperiodeDto::new).toList());
    }

    private static Trygdeavgiftsgrunnlag lagTrygdeavgiftsgrunnlag() {
        var skatteForholdINorge = new SkatteforholdTilNorge();
        skatteForholdINorge.setFomDato(LocalDate.now());
        skatteForholdINorge.setTomDato(LocalDate.now());
        skatteForholdINorge.setSkatteplikttype(Skatteplikttype.SKATTEPLIKTIG);
        var trygdeavgiftsgrunnlag = new Trygdeavgiftsgrunnlag();
        trygdeavgiftsgrunnlag.setSkatteforholdTilNorge(Collections.singletonList((skatteForholdINorge)));
        trygdeavgiftsgrunnlag.setInntektsperioder(Collections.emptyList());

        return trygdeavgiftsgrunnlag;
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
        inntektsperiode.setType(Inntektskildetype.INNTEKT_FRA_UTLANDET);
        trygdeavgift.setGrunnlagInntekstperiode(inntektsperiode);

        trygdeavgift.setFastsattTrygdeavgift(new FastsattTrygdeavgift());
        trygdeavgift.getFastsattTrygdeavgift().setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        trygdeavgift.getFastsattTrygdeavgift().getMedlemAvFolketrygden().setMedlemskapsperioder(Set.of(medlemskapsperiode));

        return Set.of(trygdeavgift);
    }
}

