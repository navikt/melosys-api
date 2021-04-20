package no.nav.melosys.service.avgift;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.*;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer;
import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningDto;
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftDto;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrygdeavgiftsberegningServiceTest {

    @Mock
    private TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    @Mock
    private MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    @Mock
    private TrygdeavgiftConsumer trygdeavgiftConsumer;

    private TrygdeavgiftsberegningService trygdeavgiftsberegningService;

    private final MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();

    private final long behandlingsresultatID = 1291;

    @BeforeEach
    public void setup() {
        medlemAvFolketrygden.setFastsattTrygdeavgift(new FastsattTrygdeavgift());
        trygdeavgiftsberegningService = new TrygdeavgiftsberegningService(trygdeavgiftsgrunnlagService, medlemAvFolketrygdenService, trygdeavgiftConsumer);
    }

    @Test
    void oppdaterBeregningsgrunnlag_medNorskOgUtenlandskAvgiftSkalIkkeBetaleAvgiftForNorskInntekt_setterIkkeAvgiftNorge() throws FunksjonellException {
        initMedlemAvFolketrygdenMock();
        var request = new OppdaterTrygdeavgiftsberegningRequest(1L, 2L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        when(trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(eq(behandlingsresultatID)))
            .thenReturn(new Trygdeavgiftsgrunnlag(Loenn_forhold.LØNN_FRA_NORGE, new AvgiftsgrunnlagInfoNorge(true, false, null, NORSK_INNTEKT_TRYGDEAVGIFT_NAV), null));
        trygdeavgiftsberegningService.oppdaterBeregningsgrunnlag(behandlingsresultatID, request);
        assertThat(medlemAvFolketrygden.getFastsattTrygdeavgift())
            .extracting(FastsattTrygdeavgift::getAvgiftspliktigUtenlandskInntektMnd, FastsattTrygdeavgift::getAvgiftspliktigNorskInntektMnd)
            .containsExactly(request.getAvgiftspliktigLønnUtland(), null);
    }

    @Test
    void oppdaterBeregningsgrunnlag_medNorskOgUtenlandskAvgiftSkalIkkeBetaleAvgiftForUtenlandskInntket_setterIkkeAvgiftUtland() throws FunksjonellException {
        initMedlemAvFolketrygdenMock();
        var request = new OppdaterTrygdeavgiftsberegningRequest(1L, 2L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV);
        when(trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(eq(behandlingsresultatID)))
            .thenReturn(new Trygdeavgiftsgrunnlag(Loenn_forhold.LØNN_FRA_NORGE, new AvgiftsgrunnlagInfoNorge(true, false, null, NORSK_INNTEKT_TRYGDEAVGIFT_NAV), null));
        trygdeavgiftsberegningService.oppdaterBeregningsgrunnlag(behandlingsresultatID, request);
        assertThat(medlemAvFolketrygden.getFastsattTrygdeavgift())
            .extracting(FastsattTrygdeavgift::getAvgiftspliktigUtenlandskInntektMnd, FastsattTrygdeavgift::getAvgiftspliktigNorskInntektMnd)
            .containsExactly(null, request.getAvgiftspliktigLønnNorge());
    }

    @Test
    void oppdaterBeregningsgrunnlag_medUtenlandskOgNorskAvgift_beregnerAvgift() throws FunksjonellException {
        initMedlemAvFolketrygdenMock();
        var request = new OppdaterTrygdeavgiftsberegningRequest(1L, 1L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setFastsattTrygdeavgift(new FastsattTrygdeavgift());

        when(trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(eq(behandlingsresultatID)))
            .thenReturn(new Trygdeavgiftsgrunnlag(
                Loenn_forhold.LØNN_FRA_UTLANDET,
                new AvgiftsgrunnlagInfoNorge(true, false, null, NORSK_INNTEKT_TRYGDEAVGIFT_NAV),
                new AvgiftsgrunnlagInfoUtland(true, false, null, UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV)));

        trygdeavgiftsberegningService.oppdaterBeregningsgrunnlag(behandlingsresultatID, request);
        assertThat(medlemAvFolketrygden.getFastsattTrygdeavgift())
            .extracting(FastsattTrygdeavgift::getAvgiftspliktigNorskInntektMnd, FastsattTrygdeavgift::getAvgiftspliktigUtenlandskInntektMnd)
            .containsExactly(request.getAvgiftspliktigLønnNorge(), request.getAvgiftspliktigLønnUtland());
    }

    @Test
    void beregnAvgift_fastsattTrygdeavgiftIkkeSatt_beregnesIkke() throws IkkeFunnetException {
        initMedlemAvFolketrygdenMock();
        medlemAvFolketrygden.setFastsattTrygdeavgift(null);
        trygdeavgiftsberegningService.beregnAvgift(behandlingsresultatID);
        verify(trygdeavgiftsgrunnlagService, never()).hentAvgiftsgrunnlag(anyLong());
    }

    @Test
    void beregnAvgift_tomtAvgiftsgrunnlag_beregnesIkke() throws IkkeFunnetException {
        initMedlemAvFolketrygdenMock();
        when(trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(eq(behandlingsresultatID))).thenReturn(new Trygdeavgiftsgrunnlag(null, null, null));
        trygdeavgiftsberegningService.beregnAvgift(behandlingsresultatID);
        verify(trygdeavgiftConsumer, never()).beregnTrygdeavgift(any());
    }

    @Test
    void beregnAvgift_tidligereBeregnetNorskAvgiftHarAvgiftspliktigUtenlandskInntekt_beregnesAvgiftAvUtenlandskInntekt() throws IkkeFunnetException {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(1000L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);

        when(trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(eq(behandlingsresultatID))).thenReturn(
            new Trygdeavgiftsgrunnlag(Loenn_forhold.LØNN_FRA_UTLANDET, null, new AvgiftsgrunnlagInfoUtland(true, false, null, UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV))
        );

        final var forventetTrygdeavgiftsbeløp = new BigDecimal("10");
        final var forventetTrygdesats = new BigDecimal("12.2");
        when(trygdeavgiftConsumer.beregnTrygdeavgift(eq(new MelosysTrygdeavgfitBeregningDto(
            false, true, medlemskapsperiode.getTrygdedekning(), medlemskapsperiode.getBestemmelse(),
            medlemAvFolketrygden.getFastsattTrygdeavgift().getAvgiftspliktigUtenlandskInntektMnd(), LocalDate.now(), null
        )))).thenReturn(new TrygdeavgiftDto("kode", forventetTrygdesats, forventetTrygdeavgiftsbeløp));

        trygdeavgiftsberegningService.beregnAvgift(behandlingsresultatID);

        assertThat(medlemskapsperiode.getTrygdeavgift())
            .hasSize(1)
            .flatExtracting(
                Trygdeavgift::erAvgiftForNorskInntekt,
                Trygdeavgift::getAvgiftskode,
                Trygdeavgift::getTrygdeavgiftsbeløpMd,
                Trygdeavgift::getTrygdesats
            ).containsExactly(
                false,
                "kode",
                forventetTrygdeavgiftsbeløp,
                forventetTrygdesats
        );
    }

    @Test
    void beregnAvgift_tidligereBeregnetNorskOgUtenlandskAvgiftHarAvgiftspliktigUtenlandskOgNorskInntekt_beregnesAvgiftAvUtenlandskOgNorskInntekt() throws IkkeFunnetException {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(false));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(1000L);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(1000L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        when(trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(eq(behandlingsresultatID))).thenReturn(
            new Trygdeavgiftsgrunnlag(
                Loenn_forhold.LØNN_FRA_UTLANDET,
                new AvgiftsgrunnlagInfoNorge(true, true, null, NORSK_INNTEKT_TRYGDEAVGIFT_NAV),
                new AvgiftsgrunnlagInfoUtland(true, false, null, UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV))
        );

        final var forventetTrygdeavgiftsbeløp = new BigDecimal("10");
        final var forventetTrygdesats = new BigDecimal("12.2");
        when(trygdeavgiftConsumer.beregnTrygdeavgift(any(MelosysTrygdeavgfitBeregningDto.class)))
            .thenReturn(new TrygdeavgiftDto("kode", forventetTrygdesats, forventetTrygdeavgiftsbeløp));

        trygdeavgiftsberegningService.beregnAvgift(behandlingsresultatID);
        verify(trygdeavgiftConsumer, times(2)).beregnTrygdeavgift(any(MelosysTrygdeavgfitBeregningDto.class));

        assertThat(medlemskapsperiode.getTrygdeavgift())
            .hasSize(2)
            .flatExtracting(
                Trygdeavgift::erAvgiftForNorskInntekt,
                Trygdeavgift::getAvgiftskode,
                Trygdeavgift::getTrygdeavgiftsbeløpMd,
                Trygdeavgift::getTrygdesats
            ).containsExactlyInAnyOrder(
                false,
                "kode",
                forventetTrygdeavgiftsbeløp,
                forventetTrygdesats,
                true,
                "kode",
                forventetTrygdeavgiftsbeløp,
                forventetTrygdesats
        );
    }

    @Test
    void hentBeregningsresultat_medNorskAvgift_validerBeregningsresultat() throws IkkeFunnetException {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(100L);

        Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = trygdeavgiftsberegningService.hentBeregningsresultat(behandlingsresultatID);
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnUtland()).isNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnNorge()).isNotNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftsperioder()).hasSize(1)
            .flatExtracting(Avgiftsperiode::getFom, Avgiftsperiode::getTom, Avgiftsperiode::getTrygdedekning, Avgiftsperiode::isForNorskInntekt)
            .containsExactly(medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(), true);
    }

    @Test
    void hentBeregningsresultat_medUtenlandskAvgift_validerBeregningsresultat() throws IkkeFunnetException {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(false));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(100L);

        Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = trygdeavgiftsberegningService.hentBeregningsresultat(behandlingsresultatID);
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnNorge()).isNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnUtland()).isNotNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftsperioder()).hasSize(1)
            .flatExtracting(Avgiftsperiode::getFom, Avgiftsperiode::getTom, Avgiftsperiode::getTrygdedekning, Avgiftsperiode::isForNorskInntekt)
            .containsExactly(medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(), false);
    }

    @Test
    void hentBeregningsresultat_medNorskOgUtenlandskAvgift_validerBeregningsresultat() throws IkkeFunnetException {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(false));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(100L);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(100L);

        Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = trygdeavgiftsberegningService.hentBeregningsresultat(behandlingsresultatID);
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnNorge()).isNotNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnUtland()).isNotNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftsperioder()).hasSize(2)
            .flatExtracting(Avgiftsperiode::getFom, Avgiftsperiode::getTom, Avgiftsperiode::getTrygdedekning, Avgiftsperiode::isForNorskInntekt)
            .containsExactlyInAnyOrder(
                medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(), false,
                medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(), true
            );
    }

    @Test
    void finnBeregningsresultat_medNorskAvgift_validerBeregningsresultat() throws IkkeFunnetException {
        when(medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)).thenReturn(Optional.of(medlemAvFolketrygden));

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(false));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(100L);

        Optional<Trygdeavgiftsberegningsresultat> resultat = trygdeavgiftsberegningService.finnBeregningsresultat(behandlingsresultatID);
        assertThat(resultat).isPresent();
        Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = resultat.get();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnNorge()).isNotNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnUtland()).isNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftsperioder()).hasSize(2)
            .flatExtracting(Avgiftsperiode::getFom, Avgiftsperiode::getTom, Avgiftsperiode::getTrygdedekning, Avgiftsperiode::isForNorskInntekt)
            .containsExactlyInAnyOrder(
                medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(), false,
                medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(), true
            );

    }

    @Test
    void finnBeregningsresultat_ikkeFunnet_tomtResultat() {
        when(medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)).thenReturn(Optional.empty());

        Optional<Trygdeavgiftsberegningsresultat> resultat = trygdeavgiftsberegningService.finnBeregningsresultat(behandlingsresultatID);

        assertThat(resultat).isNotPresent();
    }

    private void initMedlemAvFolketrygdenMock() throws IkkeFunnetException {
        when(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)).thenReturn(medlemAvFolketrygden);
    }

    private Medlemskapsperiode lagMedlemskapsperiode() {
        Medlemskapsperiode medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.HELSEDEL);
        medlemskapsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        medlemskapsperiode.setFom(LocalDate.now());
        medlemskapsperiode.setTom(LocalDate.now().plusYears(1));
        return medlemskapsperiode;
    }

    private Trygdeavgift lagTrygdeavgift(boolean forNorskInntekt) {
        Trygdeavgift trygdeavgift = new Trygdeavgift();
        trygdeavgift.setAvgiftForInntekt(forNorskInntekt ? Trygdeavgift.AvgiftForInntekt.NORSK_INNTEKT : Trygdeavgift.AvgiftForInntekt.UTENLANDSK_INNTEKT);
        trygdeavgift.setTrygdeavgiftsbeløpMd(new BigDecimal(10));
        trygdeavgift.setAvgiftskode("ABC");
        trygdeavgift.setTrygdesats(new BigDecimal("1.1"));
        return trygdeavgift;
    }
}
