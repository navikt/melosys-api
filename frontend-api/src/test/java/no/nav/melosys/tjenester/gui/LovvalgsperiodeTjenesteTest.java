package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.lovvalgsperiode.OpprettLovvalgsperiodeService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {LovvalgsperiodeTjeneste.class})
final class LovvalgsperiodeTjenesteTest {
    private static final LocalDate FOM = LocalDate.now();
    private static final LovvalgsperiodeDto FORVENTET = new LovvalgsperiodeDto("1",
        new PeriodeDto(FOM, FOM),
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2,
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
        Land_iso2.SK,
        InnvilgelsesResultat.AVSLAATT,
        Trygdedekninger.FULL_DEKNING_EOSFO,
        Medlemskapstyper.FRIVILLIG,
        "10");

    private static final String BASE_URL = "/api/lovvalgsperioder";

    @MockBean
    private LovvalgsperiodeService lovvalgsperiodeService;
    @MockBean
    private Aksesskontroll aksesskontroll;
    @MockBean
    private OpprettLovvalgsperiodeService opprettLovvalgsperiodeService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void hentEksisterendeLovvalgsperiodeGir200OkOgEnForekomst() throws Exception {
        when(lovvalgsperiodeService.hentLovvalgsperioder(13L)).thenReturn(lagLovvalgsperiode());
        testHentLovvalgsperioder(13L, Collections.singletonList(FORVENTET));
        verify(aksesskontroll).autoriser(13L);
    }

    @Test
    void hentIkkeEksisterendeLovvalgsperiodeGir200OkOgTomJson() throws Exception {
        when(lovvalgsperiodeService.hentLovvalgsperioder(Long.MAX_VALUE)).thenReturn(Collections.emptyList());
        testHentLovvalgsperioder(Long.MAX_VALUE, Collections.emptyList());
        verify(aksesskontroll).autoriser(Long.MAX_VALUE);
    }

    @Test
    void hentLovvalgsperiodeUtenTilgang() throws Exception {
        doThrow(new SikkerhetsbegrensningException("Computer says no")).when(aksesskontroll).autoriser(10L);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}", 10L)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is4xxClientError())
            .andExpect(responseBody(objectMapper).containsError("message", "Computer says no"));

        verify(aksesskontroll).autoriser(10L);
    }

    @Test
    void hentLovvalgsperiodeMedTekniskFeil() throws Exception {
        doThrow(new TekniskException("Det har oppstått en...")).when(aksesskontroll).autoriser(15L);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}", 15L)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is5xxServerError())
            .andExpect(responseBody(objectMapper).containsError("message", "Det har oppstått en..."));

        verify(aksesskontroll).autoriser(15L);
    }

    @Test
    void hentOpprinneligLovvalgsperiode_returnererPeriode() throws Exception {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        LocalDate fomDato = LocalDate.of(2018, 12, 12);
        LocalDate tomDato = LocalDate.of(2019, 12, 12);
        lovvalgsperiode.setFom(fomDato);
        lovvalgsperiode.setTom(tomDato);

        when(lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(5L)).thenReturn(lovvalgsperiode);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}/opprinnelig", 5L)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.opprinneligLovvalgsperiode.fom", equalTo("2018-12-12")))
            .andExpect(jsonPath("$.opprinneligLovvalgsperiode.tom", equalTo("2019-12-12")));

        verify(aksesskontroll).autoriser(5L);
    }

    @Test
    void lagreEnLovvalgsperiodeGir200OkOgEkko() throws Exception {
        List<LovvalgsperiodeDto> lovvalgsperiodeDtos = Collections.singletonList(FORVENTET);
        mockMvc.perform(post(BASE_URL + "/{behandlingID}", 42L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lovvalgsperiodeDtos))
            )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(lovvalgsperiodeDtos, new TypeReference<List<LovvalgsperiodeDto>>() {
            }));

        var lovvalgsperiodeSomBlirLagret = lagLovvalgsperiode();
        lovvalgsperiodeSomBlirLagret.get(0).setId(null);
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(42L, lovvalgsperiodeSomBlirLagret);

        verify(aksesskontroll).autoriserSkriv(42L);
    }

    private void testHentLovvalgsperioder(long behandlingsid, Collection<LovvalgsperiodeDto> forventet) throws Exception {
        mockMvc.perform(get(BASE_URL + "/{behandlingID}", behandlingsid)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(forventet.size())))
            .andExpect(responseBody(objectMapper).containsObjectAsJson(forventet, new TypeReference<List<LovvalgsperiodeDto>>() {
            }));

    }

    private List<Lovvalgsperiode> lagLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setId(Long.parseLong(FORVENTET.periodeID));
        lovvalgsperiode.setFom(FORVENTET.periode.fom);
        lovvalgsperiode.setTom(FORVENTET.periode.tom);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setLovvalgsland(Land_iso2.valueOf(FORVENTET.lovvalgsland));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.valueOf(FORVENTET.lovvalgsbestemmelse));
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.valueOf(FORVENTET.tilleggBestemmelse));
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.valueOf(FORVENTET.innvilgelsesResultat));
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.valueOf(FORVENTET.medlemskapstype));
        lovvalgsperiode.setMedlPeriodeID(Long.valueOf(FORVENTET.medlemskapsperiodeID));

        return Collections.singletonList(lovvalgsperiode);
    }
}
