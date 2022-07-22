package no.nav.melosys.tjenester.gui;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.AvklartefaktaOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.LagreMedfolgendeFamilieDto;
import no.nav.melosys.tjenester.gui.dto.MedfolgendeFamilieDto;
import no.nav.melosys.tjenester.gui.dto.VirksomheterDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN;
import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AvklartefaktaTjeneste.class})
@ExtendWith(MockitoExtension.class)
class AvklartefaktaTjenesteTest {

    @MockBean
    private AvklartefaktaService avklartefaktaService;
    @MockBean
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @MockBean
    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/avklartefakta";
    private static final String uuid1 = "36053ce6-75e5-4430-b8af-2ce60092877d";
    private static final String uuid2 = "e502441e-9cdd-4d2a-84c2-25261b6e7cb2";
    private static final String uuid3 = "d7645947-e7e9-46c0-987a-d0e91d6fed6f";
    private static final String uuid4 = "4136cdce-0c09-4693-a032-5914575c3ac3";

    @Test
    void hentAvklarteFakta() throws Exception {
        Set<AvklartefaktaDto> dtos = lagAvklarteFaktaDtoSet();
        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(dtos);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void lagreAvklarteFakta() throws Exception {
        Set<AvklartefaktaDto> dtos = lagAvklarteFaktaDtoSet();
        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(dtos);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtos)))
            .andExpect(status().isOk());
    }

    @Test
    void hentAvklarteFaktaStrukturert() throws Exception {
        Set<AvklartefaktaDto> dtos = lagAvklarteFaktaDtoSet();
        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(dtos);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}/oppsummering", 1L)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(AvklartefaktaOppsummeringDto.av(dtos), AvklartefaktaOppsummeringDto.class));
    }

    @Test
    void lagreVirksomheterSomAvklarteFakta() throws Exception {
        var virksomheterDto = new VirksomheterDto();
        virksomheterDto.setVirksomhetIDer(Collections.singletonList("000000000"));
        Set<AvklartefaktaDto> dtos = lagAvklarteFaktaDtoSet();
        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(dtos);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/virksomheter", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(virksomheterDto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(AvklartefaktaOppsummeringDto.av(dtos), AvklartefaktaOppsummeringDto.class));
    }

    @Test
    void lagreMedfolgendeFamilieSomAvklarteFakta() throws Exception {
        LagreMedfolgendeFamilieDto lagreMedfolgendeFamilieDto = new LagreMedfolgendeFamilieDto(lagMedfolgendeFamilieDtoSet());
        Set<AvklartefaktaDto> dtos = lagAvklarteFaktaDtoSet();
        when(avklartefaktaService.hentAlleAvklarteFakta(eq(1L))).thenReturn(dtos);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/medfolgendeFamilie", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lagreMedfolgendeFamilieDto)))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(AvklartefaktaOppsummeringDto.av(dtos), AvklartefaktaOppsummeringDto.class));
    }

    private static Set<MedfolgendeFamilieDto> lagMedfolgendeFamilieDtoSet() {
        return Set.of(
            new MedfolgendeFamilieDto(uuid1, true, null, null),
            new MedfolgendeFamilieDto(uuid2, false, OVER_18_AR.getKode(), "fritekstForUuid2"),
            new MedfolgendeFamilieDto(uuid3, true, null, null),
            new MedfolgendeFamilieDto(uuid4, false, SAMBOER_UTEN_FELLES_BARN.getKode(), "fritekstForUuid4"));
    }

    private static Set<AvklartefaktaDto> lagAvklarteFaktaDtoSet() {
        return Set.of(
            lagAvklartefaktaDto(uuid1, Avklartefaktatyper.VURDERING_LOVVALG_BARN, true, null, null),
            lagAvklartefaktaDto(uuid2, Avklartefaktatyper.VURDERING_LOVVALG_BARN, false, "fritekstForUuid2", OVER_18_AR.getKode()),
            lagAvklartefaktaDto(uuid3, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, true, null, null),
            lagAvklartefaktaDto(uuid4, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, false, "fritekstForUuid4", SAMBOER_UTEN_FELLES_BARN.getKode()));
    }

    private static AvklartefaktaDto lagAvklartefaktaDto(String subjektID, Avklartefaktatyper type, boolean fakta, String begrunnelseFritekst, String begrunnelsekode) {
        Avklartefakta avklartefakta = new Avklartefakta();
        avklartefakta.setSubjekt(subjektID);
        avklartefakta.setType(type);
        avklartefakta.setReferanse(type.getKode());
        avklartefakta.setBegrunnelseFritekst(begrunnelseFritekst);
        if (fakta) {
            avklartefakta.setFakta(Avklartefakta.VALGT_FAKTA);
        } else {
            avklartefakta.setFakta(Avklartefakta.IKKE_VALGT_FAKTA);
            AvklartefaktaRegistrering registrering = new AvklartefaktaRegistrering();
            registrering.setAvklartefakta(avklartefakta);
            registrering.setBegrunnelseKode(begrunnelsekode);
            avklartefakta.setRegistreringer(new HashSet<>(List.of(registrering)));
        }
        return new AvklartefaktaDto(avklartefakta);
    }
}

