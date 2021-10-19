package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadFtrl;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
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
    @Mock
    private AvklarteMedfolgendeFamilieService avklarteMedfolgendeFamilieService;
    @Mock
    private AvklarteVirksomheterService avklarteVirksomheterService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private TrygdeavtaleTjeneste trygdeavtaleTjeneste;

    @BeforeEach
    void init() {
        trygdeavtaleTjeneste = new TrygdeavtaleTjeneste(
            trygdeavtaleService,
            behandlingService,
            aksesskontroll,
            behandlingsgrunnlagService,
            avklarteMedfolgendeFamilieService,
            avklarteVirksomheterService,
            lovvalgsperiodeService);
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
    void leggInnTrygdeAvtaleDataForOgKunneFatteVetak() throws NoSuchFieldException, IllegalAccessException {
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(1L)).thenReturn(lagBehandlingsgrunnlag());

        TrygdeAvtaleDataForVedtakDto trygdeAvtaleDataForVedtakDto = new TrygdeAvtaleDataForVedtakDto.Builder()
            .fom(LocalDate.of(2021, 1, 1))
            .tom(LocalDate.of(2021, 2, 1))
            .land(List.of("GB"))
            .virksomheter(List.of("11111111111"))
            .vedtak("JA_FATTE_VEDTAK")
            .innvilgelse("JA")
            .bestemmelse("UK_ART6_1")
            .addBarn("0bad5c70-8a3f-4fc7-9031-d3aebd6b68de",
                false, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
                "begrunnelse barn")
            .ektefelle("1212121212121-4fc7-9031-ab34332121ff",
                false, Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(),
                "begrunnelse samboer")
            .build();

        trygdeavtaleTjeneste.overforDataForVedtak(1L, trygdeAvtaleDataForVedtakDto);

        verify(behandlingsgrunnlagService, never()).oppdaterBehandlingsgrunnlag(any());
        verify(avklarteMedfolgendeFamilieService).lagreMedfolgendeFamilieSomAvklartefakta(anyLong(), any());
        verify(avklarteVirksomheterService).lagreVirksomheterSomAvklartefakta(any(), anyLong());
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(anyLong(), any());
    }

    private Behandlingsgrunnlag lagBehandlingsgrunnlag() throws NoSuchFieldException, IllegalAccessException {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        Field field = behandlingsgrunnlag.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(behandlingsgrunnlag, 1L);
        SoeknadFtrl behandlingsgrunnlagdata = new SoeknadFtrl();
        behandlingsgrunnlagdata.soeknadsland.landkoder.add("GB");
        behandlingsgrunnlagdata.periode = new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1));
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagdata);
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
