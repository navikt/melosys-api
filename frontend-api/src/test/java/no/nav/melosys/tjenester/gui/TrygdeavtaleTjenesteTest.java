package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeAvtaleDataForVedtakDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrygdeavtaleTjenesteTest {

    @Mock
    private TrygdeavtaleService trygdeavtaleService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private Aksesskontroll aksesskontroll;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;

    private TrygdeavtaleTjeneste trygdeavtaleTjeneste;

    @BeforeEach
    void init() {
        trygdeavtaleTjeneste = new TrygdeavtaleTjeneste(trygdeavtaleService, behandlingService, aksesskontroll, behandlingsgrunnlagService);
    }

    @Test
    void hentTrygdeavtaleInfo_utenVirksomhetOgBarnEktefelle_returnererKorrekt() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagBehandling());

        trygdeavtaleTjeneste.hentTrygdeavtaleInfo(1L, false, false);

        verify(trygdeavtaleService, never()).hentVirksomheter(any());
        verify(trygdeavtaleService, never()).hentFamiliemedlemmer(any());
    }

    @Test
    void hentTrygdeavtaleInfo_medVirksomhetOgBarnEktefelle_returnererKorrekt() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagBehandling());

        trygdeavtaleTjeneste.hentTrygdeavtaleInfo(1L, true, true);

        verify(trygdeavtaleService).hentVirksomheter(any());
        verify(trygdeavtaleService).hentFamiliemedlemmer(any());
    }

    @Test
    void leggInnTrygdeAvtaleDataForOgKunneFatteVetak() throws JsonProcessingException, NoSuchFieldException, IllegalAccessException {
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(1L)).thenReturn(lagBehandlingsgrunnlag());

        TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto = new TrygdeAvtaleDataForVedtakDto.Builder()
            .fom(LocalDate.of(2021, 1, 1))
            .tom(LocalDate.of(2021, 2, 1))
            .land(List.of("GB"))
            .virksomheter(List.of("11111111111"))
            .vedtak("JA_FATTE_VEDTAK")
            .innvilgelse("JA")
            .bestemmelse("UK_ART6_1")
            .addBarn("0bad5c70-8a3f-4fc7-9031-d3aebd6b68de", false, null, null)
            .ektefelle("0bad5c70-8a3f-4fc7-9031-d3aebd6b68de", false,"SAMBOER_UTEN_FELLES_BARN" , "fritekst")
            .build();

        trygdeavtaleTjeneste.overforDataForVedtak(1L, trygdeAvtaleDataForVedtakDto);

        verify(behandlingsgrunnlagService).oppdaterBehandlingsgrunnlag(any());
    }

    private Behandlingsgrunnlag lagBehandlingsgrunnlag() throws NoSuchFieldException, IllegalAccessException {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        Field field = behandlingsgrunnlag.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(behandlingsgrunnlag, 1L);
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new SoeknadFtrl());
        return behandlingsgrunnlag;
    }

    private Behandling lagBehandling() {
        var bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        var fagsak = new Fagsak();
        fagsak.getAktører().add(bruker);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        return behandling;
    }
}
