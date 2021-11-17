package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.TrygdeavtaleService;
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleResultatDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    private static final Behandling behandling = lagBehandling();

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
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);

        var response = trygdeavtaleTjeneste.hentTrygdeavtaleInfo(1L, false, false).getBody();

        verify(trygdeavtaleService, never()).hentVirksomheter(any());
        verify(trygdeavtaleService, never()).hentFamiliemedlemmer(any());

        assertThat(response).isNotNull();
        assertThat(response.aktoerId()).isEqualTo(behandling.getFagsak().hentAktørID());
        assertThat(response.behandlingstema()).isEqualTo(behandling.getTema().getKode());
        var behandlingsgrunnlagdata = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        assertThat(response.periodeFom()).isEqualTo(behandlingsgrunnlagdata.periode.getFom());
        assertThat(response.periodeTom()).isEqualTo(behandlingsgrunnlagdata.periode.getTom());
        assertThat(response.soeknadsland()).isEqualTo(behandlingsgrunnlagdata.soeknadsland.landkoder);
    }

    @Test
    void hentTrygdeavtaleInfo_medVirksomhetOgBarnEktefelle_returnererKorrekt() {
        when(behandlingService.hentBehandling(1L)).thenReturn(behandling);

        var response = trygdeavtaleTjeneste.hentTrygdeavtaleInfo(1L, true, true).getBody();

        verify(trygdeavtaleService).hentVirksomheter(any());
        verify(trygdeavtaleService).hentFamiliemedlemmer(any());

        assertThat(response).isNotNull();
        assertThat(response.aktoerId()).isEqualTo(behandling.getFagsak().hentAktørID());
        assertThat(response.behandlingstema()).isEqualTo(behandling.getTema().getKode());
        var behandlingsgrunnlagdata = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
        assertThat(response.periodeFom()).isEqualTo(behandlingsgrunnlagdata.periode.getFom());
        assertThat(response.periodeTom()).isEqualTo(behandlingsgrunnlagdata.periode.getTom());
        assertThat(response.soeknadsland()).isEqualTo(behandlingsgrunnlagdata.soeknadsland.landkoder);
    }

    @Test
    void leggInnTrygdeAvtaleDataForOgKunneFatteVetak() {
        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(1L)).thenReturn(lagBehandlingsgrunnlag());

        TrygdeavtaleResultatDto trygdeavtaleResultatDto = lagTrygdeavtaleResultatDto();

        trygdeavtaleTjeneste.overførResultat(1L, trygdeavtaleResultatDto);

        verify(behandlingsgrunnlagService, never()).oppdaterBehandlingsgrunnlag(any());
        verify(avklarteMedfolgendeFamilieService).lagreMedfolgendeFamilieSomAvklartefakta(anyLong(), any());
        verify(avklarteVirksomheterService).lagreVirksomheterSomAvklartefakta(any(), anyLong());
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(1L, expectedLovvalgsperioder());
    }

    @Test
    void overførResultat_medToLandkoder_kasterTekniskException() {
        Behandlingsgrunnlag behandlingsgrunnlag = lagBehandlingsgrunnlag();
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland.landkoder = List.of("GB", "NO");

        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(1L)).thenReturn(behandlingsgrunnlag);
        TrygdeavtaleResultatDto trygdeavtaleResultatDto = lagTrygdeavtaleResultatDto();

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() ->trygdeavtaleTjeneste.overførResultat(1L, trygdeavtaleResultatDto))
            .withMessageContaining("Forventet ett land i behandlingsgrunnlagdata soeknadsland.landkoder, men fant: [GB, NO]");
    }

    @Test
    void overførResultat_manglerLandkoder_kasterTekniskException() {
        Behandlingsgrunnlag behandlingsgrunnlag = lagBehandlingsgrunnlag();
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland.landkoder = List.of();

        when(behandlingsgrunnlagService.hentBehandlingsgrunnlag(1L)).thenReturn(behandlingsgrunnlag);
        TrygdeavtaleResultatDto trygdeavtaleResultatDto = lagTrygdeavtaleResultatDto();

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() ->trygdeavtaleTjeneste.overførResultat(1L, trygdeavtaleResultatDto))
            .withMessageContaining("Forventet ett land i behandlingsgrunnlagdata soeknadsland.landkoder, men fant: []");
    }

    private TrygdeavtaleResultatDto lagTrygdeavtaleResultatDto() {
        return new TrygdeavtaleResultatDto.Builder()
            .virksomheter(List.of("11111111111"))
            .bestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1.getKode())
            .addBarn("0bad5c70-8a3f-4fc7-9031-d3aebd6b68de",
                false, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
                "begrunnelse barn")
            .ektefelle("1212121212121-4fc7-9031-ab34332121ff",
                false, Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(),
                "begrunnelse samboer")
            .build();
    }

    private Collection<Lovvalgsperiode> expectedLovvalgsperioder() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LocalDate.of(2020, 1, 1));
        lovvalgsperiode.setTom(LocalDate.of(2021, 1, 1));
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        lovvalgsperiode.setLovvalgsland(Landkoder.GB);
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.PLIKTIG);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        return List.of(lovvalgsperiode);
    }

    private static Behandlingsgrunnlag lagBehandlingsgrunnlag() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        SoeknadTrygdeavtale behandlingsgrunnlagdata = new SoeknadTrygdeavtale();
        behandlingsgrunnlagdata.soeknadsland.landkoder.add("GB");
        behandlingsgrunnlagdata.periode = new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagdata);
        return behandlingsgrunnlag;
    }

    private static Behandling lagBehandling() {
        var bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("AktørId");
        var fagsak = new Fagsak();
        fagsak.getAktører().add(bruker);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setBehandlingsgrunnlag(lagBehandlingsgrunnlag());
        return behandling;
    }
}
