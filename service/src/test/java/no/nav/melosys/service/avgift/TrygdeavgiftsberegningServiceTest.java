package no.nav.melosys.service.avgift;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.*;
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer;
import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningV1Dto;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrygdeavgiftsberegningServiceTest {

    @Mock
    private TrygdeavgiftsgrunnlagServiceDeprecated trygdeavgiftsgrunnlagServiceDeprecated;
    @Mock
    private MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    @Mock
    private TrygdeavgiftConsumer trygdeavgiftConsumer;

    private TrygdeavgiftsberegningServiceDeprecated trygdeavgiftsberegningServiceDeprecated;

    private final MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();

    private final long behandlingsresultatID = 1291;

    @BeforeEach
    public void setup() {
        medlemAvFolketrygden.setFastsattTrygdeavgift(new FastsattTrygdeavgift());
        trygdeavgiftsberegningServiceDeprecated = new TrygdeavgiftsberegningServiceDeprecated(trygdeavgiftsgrunnlagServiceDeprecated, medlemAvFolketrygdenService, trygdeavgiftConsumer);
    }

    @Test
    void oppdaterBeregningsgrunnlag_medNorskOgUtenlandskAvgiftSkalIkkeBetaleAvgiftForNorskInntekt_setterIkkeAvgiftNorge() {
        initMedlemAvFolketrygdenMock();
        var request = new OppdaterTrygdeavgiftsberegningRequest(1L, 2L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        when(trygdeavgiftsgrunnlagServiceDeprecated.hentAvgiftsgrunnlag(eq(behandlingsresultatID)))
            .thenReturn(new TrygdeavgiftsgrunnlagDeprecated(Loenn_forhold.LØNN_FRA_NORGE, new AvgiftsgrunnlagInfoNorge(true, false, null, NORSK_INNTEKT_TRYGDEAVGIFT_NAV), null));
        trygdeavgiftsberegningServiceDeprecated.oppdaterBeregningsgrunnlag(behandlingsresultatID, request);
        assertThat(medlemAvFolketrygden.getFastsattTrygdeavgift())
            .extracting(FastsattTrygdeavgift::getAvgiftspliktigUtenlandskInntektMnd, FastsattTrygdeavgift::getAvgiftspliktigNorskInntektMnd)
            .containsExactly(request.getAvgiftspliktigLønnUtland(), null);
    }

    @Test
    void oppdaterBeregningsgrunnlag_medNorskOgUtenlandskAvgiftSkalIkkeBetaleAvgiftForUtenlandskInntket_setterIkkeAvgiftUtland() {
        initMedlemAvFolketrygdenMock();
        var request = new OppdaterTrygdeavgiftsberegningRequest(1L, 2L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV);
        when(trygdeavgiftsgrunnlagServiceDeprecated.hentAvgiftsgrunnlag(eq(behandlingsresultatID)))
            .thenReturn(new TrygdeavgiftsgrunnlagDeprecated(Loenn_forhold.LØNN_FRA_NORGE, new AvgiftsgrunnlagInfoNorge(true, false, null, NORSK_INNTEKT_TRYGDEAVGIFT_NAV), null));
        trygdeavgiftsberegningServiceDeprecated.oppdaterBeregningsgrunnlag(behandlingsresultatID, request);
        assertThat(medlemAvFolketrygden.getFastsattTrygdeavgift())
            .extracting(FastsattTrygdeavgift::getAvgiftspliktigUtenlandskInntektMnd, FastsattTrygdeavgift::getAvgiftspliktigNorskInntektMnd)
            .containsExactly(null, request.getAvgiftspliktigLønnNorge());
    }

    @Test
    void oppdaterBeregningsgrunnlag_medUtenlandskOgNorskAvgift_beregnerAvgift() {
        initMedlemAvFolketrygdenMock();
        var request = new OppdaterTrygdeavgiftsberegningRequest(1L, 1L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setFastsattTrygdeavgift(new FastsattTrygdeavgift());

        when(trygdeavgiftsgrunnlagServiceDeprecated.hentAvgiftsgrunnlag(eq(behandlingsresultatID)))
            .thenReturn(new TrygdeavgiftsgrunnlagDeprecated(
                Loenn_forhold.LØNN_FRA_UTLANDET,
                new AvgiftsgrunnlagInfoNorge(true, false, null, NORSK_INNTEKT_TRYGDEAVGIFT_NAV),
                new AvgiftsgrunnlagInfoUtland(true, false, null, UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV)));

        trygdeavgiftsberegningServiceDeprecated.oppdaterBeregningsgrunnlag(behandlingsresultatID, request);
        assertThat(medlemAvFolketrygden.getFastsattTrygdeavgift())
            .extracting(FastsattTrygdeavgift::getAvgiftspliktigNorskInntektMnd, FastsattTrygdeavgift::getAvgiftspliktigUtenlandskInntektMnd)
            .containsExactly(request.getAvgiftspliktigLønnNorge(), request.getAvgiftspliktigLønnUtland());
    }

    @Test
    void beregnAvgift_fastsattTrygdeavgiftIkkeSatt_beregnesIkke() {
        initMedlemAvFolketrygdenMock();
        medlemAvFolketrygden.setFastsattTrygdeavgift(null);
        trygdeavgiftsberegningServiceDeprecated.beregnAvgift(behandlingsresultatID);
        verify(trygdeavgiftsgrunnlagServiceDeprecated, never()).hentAvgiftsgrunnlag(anyLong());
    }

    @Test
    void beregnAvgift_tomtAvgiftsgrunnlag_beregnesIkke() {
        initMedlemAvFolketrygdenMock();
        when(trygdeavgiftsgrunnlagServiceDeprecated.hentAvgiftsgrunnlag(eq(behandlingsresultatID))).thenReturn(new TrygdeavgiftsgrunnlagDeprecated(null, null, null));
        trygdeavgiftsberegningServiceDeprecated.beregnAvgift(behandlingsresultatID);
        verify(trygdeavgiftConsumer, never()).beregnTrygdeavgift(any(MelosysTrygdeavgfitBeregningV1Dto.class));
    }

    @Test
    void beregnAvgift_tidligereBeregnetNorskAvgiftHarAvgiftspliktigUtenlandskInntekt_beregnesAvgiftAvUtenlandskInntekt() {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(1000L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);

        when(trygdeavgiftsgrunnlagServiceDeprecated.hentAvgiftsgrunnlag(eq(behandlingsresultatID))).thenReturn(
            new TrygdeavgiftsgrunnlagDeprecated(Loenn_forhold.LØNN_FRA_UTLANDET, null, new AvgiftsgrunnlagInfoUtland(true, false, null, UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV))
        );

        final var forventetTrygdeavgiftsbeløp = new BigDecimal("10");
        final var forventetTrygdesats = new BigDecimal("12.2");
        when(trygdeavgiftConsumer.beregnTrygdeavgift(eq(new MelosysTrygdeavgfitBeregningV1Dto(
            false, true, medlemskapsperiode.getTrygdedekning(), medlemskapsperiode.getBestemmelse(),
            medlemAvFolketrygden.getFastsattTrygdeavgift().getAvgiftspliktigUtenlandskInntektMnd(), null,
            medlemskapsperiode.getFom(), medlemskapsperiode.getTom())))).thenReturn(Collections.singletonList(new TrygdeavgiftDto(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 1), "kode", forventetTrygdesats, forventetTrygdeavgiftsbeløp)));

        trygdeavgiftsberegningServiceDeprecated.beregnAvgift(behandlingsresultatID);

        assertThat(medlemskapsperiode.getTrygdeavgift())
            .hasSize(1)
            .flatExtracting(
                TrygdeavgiftDeprecated::erAvgiftForNorskInntekt,
                TrygdeavgiftDeprecated::getAvgiftskode,
                TrygdeavgiftDeprecated::getTrygdeavgiftsbeløpMd,
                TrygdeavgiftDeprecated::getTrygdesats
            ).containsExactly(
                false,
                "kode",
                forventetTrygdeavgiftsbeløp,
                forventetTrygdesats
            );
    }

    @Test
    void beregnAvgift_tidligereBeregnetNorskOgUtenlandskAvgiftHarAvgiftspliktigUtenlandskOgNorskInntekt_beregnesAvgiftAvUtenlandskOgNorskInntekt() {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(false));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(1000L);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(1000L);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        when(trygdeavgiftsgrunnlagServiceDeprecated.hentAvgiftsgrunnlag(eq(behandlingsresultatID))).thenReturn(
            new TrygdeavgiftsgrunnlagDeprecated(
                Loenn_forhold.LØNN_FRA_UTLANDET,
                new AvgiftsgrunnlagInfoNorge(true, true, null, NORSK_INNTEKT_TRYGDEAVGIFT_NAV),
                new AvgiftsgrunnlagInfoUtland(true, false, null, UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV))
        );

        final var forventetTrygdeavgiftsbeløp = new BigDecimal("10");
        final var forventetTrygdesats = new BigDecimal("12.2");
        when(trygdeavgiftConsumer.beregnTrygdeavgift(any(MelosysTrygdeavgfitBeregningV1Dto.class)))
            .thenReturn(Collections.singletonList(new TrygdeavgiftDto(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 1), "kode", forventetTrygdesats, forventetTrygdeavgiftsbeløp)));

        trygdeavgiftsberegningServiceDeprecated.beregnAvgift(behandlingsresultatID);
        verify(trygdeavgiftConsumer, times(2)).beregnTrygdeavgift(any(MelosysTrygdeavgfitBeregningV1Dto.class));

        assertThat(medlemskapsperiode.getTrygdeavgift())
            .hasSize(2)
            .flatExtracting(
                TrygdeavgiftDeprecated::erAvgiftForNorskInntekt,
                TrygdeavgiftDeprecated::getAvgiftskode,
                TrygdeavgiftDeprecated::getTrygdeavgiftsbeløpMd,
                TrygdeavgiftDeprecated::getTrygdesats
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
    void hentBeregningsresultat_medNorskAvgift_validerBeregningsresultat() {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(100L);

        Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = trygdeavgiftsberegningServiceDeprecated.hentBeregningsresultat(behandlingsresultatID);
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnUtland()).isNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnNorge()).isNotNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftsperioder()).hasSize(1)
            .flatExtracting(Avgiftsperiode::getFom, Avgiftsperiode::getTom, Avgiftsperiode::getTrygdedekning, Avgiftsperiode::isForNorskInntekt)
            .containsExactly(medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(), true);
    }

    @Test
    void hentBeregningsresultat_medUtenlandskAvgift_validerBeregningsresultat() {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(false));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(100L);

        Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = trygdeavgiftsberegningServiceDeprecated.hentBeregningsresultat(behandlingsresultatID);
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnNorge()).isNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftspliktigLønnUtland()).isNotNull();
        assertThat(trygdeavgiftsberegningsresultat.getAvgiftsperioder()).hasSize(1)
            .flatExtracting(Avgiftsperiode::getFom, Avgiftsperiode::getTom, Avgiftsperiode::getTrygdedekning, Avgiftsperiode::isForNorskInntekt)
            .containsExactly(medlemskapsperiode.getFom(), medlemskapsperiode.getTom(), medlemskapsperiode.getTrygdedekning(), false);
    }

    @Test
    void hentBeregningsresultat_medNorskOgUtenlandskAvgift_validerBeregningsresultat() {
        initMedlemAvFolketrygdenMock();

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(false));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.setVurderingTrygdeavgiftUtenlandskInntekt(UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(100L);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(100L);

        Trygdeavgiftsberegningsresultat trygdeavgiftsberegningsresultat = trygdeavgiftsberegningServiceDeprecated.hentBeregningsresultat(behandlingsresultatID);
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
    void finnBeregningsresultat_medNorskAvgift_validerBeregningsresultat() {
        when(medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)).thenReturn(Optional.of(medlemAvFolketrygden));

        var medlemskapsperiode = lagMedlemskapsperiode();
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(true));
        medlemskapsperiode.getTrygdeavgift().add(lagTrygdeavgift(false));
        medlemAvFolketrygden.getMedlemskapsperioder().add(medlemskapsperiode);
        medlemAvFolketrygden.setVurderingTrygdeavgiftNorskInntekt(NORSK_INNTEKT_TRYGDEAVGIFT_NAV);
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(100L);

        Optional<Trygdeavgiftsberegningsresultat> resultat = trygdeavgiftsberegningServiceDeprecated.finnBeregningsresultat(behandlingsresultatID);
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

        Optional<Trygdeavgiftsberegningsresultat> resultat = trygdeavgiftsberegningServiceDeprecated.finnBeregningsresultat(behandlingsresultatID);

        assertThat(resultat).isNotPresent();
    }

    private void initMedlemAvFolketrygdenMock() {
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

    private TrygdeavgiftDeprecated lagTrygdeavgift(boolean forNorskInntekt) {
        TrygdeavgiftDeprecated trygdeavgiftDeprecated = new TrygdeavgiftDeprecated();
        trygdeavgiftDeprecated.setAvgiftForInntekt(forNorskInntekt ? TrygdeavgiftDeprecated.AvgiftForInntekt.NORSK_INNTEKT : TrygdeavgiftDeprecated.AvgiftForInntekt.UTENLANDSK_INNTEKT);
        trygdeavgiftDeprecated.setTrygdeavgiftsbeløpMd(new BigDecimal(10));
        trygdeavgiftDeprecated.setAvgiftskode("ABC");
        trygdeavgiftDeprecated.setTrygdesats(new BigDecimal("1.1"));
        trygdeavgiftDeprecated.setPeriodeFra(LocalDate.now());
        trygdeavgiftDeprecated.setPeriodeTil(LocalDate.now().plusYears(1));
        return trygdeavgiftDeprecated;
    }
}
