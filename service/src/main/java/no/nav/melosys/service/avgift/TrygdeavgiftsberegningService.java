package no.nav.melosys.service.avgift;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfo;
import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsberegningRequest;
import no.nav.melosys.domain.avgift.TrygdeavgiftDeprecated;
import no.nav.melosys.domain.avgift.Trygdeavgiftsberegningsresultat;
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer;
import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningDto;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV;

@Service
public class TrygdeavgiftsberegningService {

    private final TrygdeavgiftsgrunnlagServiceDeprecated trygdeavgiftsgrunnlagServiceDeprecated;
    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    private final TrygdeavgiftConsumer trygdeavgiftConsumer;

    public TrygdeavgiftsberegningService(TrygdeavgiftsgrunnlagServiceDeprecated trygdeavgiftsgrunnlagServiceDeprecated,
                                         MedlemAvFolketrygdenService medlemAvFolketrygdenService, TrygdeavgiftConsumer trygdeavgiftConsumer) {
        this.trygdeavgiftsgrunnlagServiceDeprecated = trygdeavgiftsgrunnlagServiceDeprecated;
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
        this.trygdeavgiftConsumer = trygdeavgiftConsumer;
    }

    @Transactional
    public void oppdaterBeregningsgrunnlag(long behandlingsresultatID, OppdaterTrygdeavgiftsberegningRequest request) {
        final var medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID);

        if (medlemAvFolketrygden.getVurderingTrygdeavgiftNorskInntekt() == NORSK_INNTEKT_TRYGDEAVGIFT_NAV) {
            medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(request.getAvgiftspliktigLønnNorge());
        } else {
            medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(null);
        }

        if (medlemAvFolketrygden.getVurderingTrygdeavgiftUtenlandskInntekt() == UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV) {
            medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(request.getAvgiftspliktigLønnUtland());
        } else {
            medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(null);
        }

        beregnAvgift(behandlingsresultatID);
    }

    @Transactional
    public void beregnAvgift(long behandlingsresultatID) {
        final var medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID);
        final var medlemskapsperioder = medlemAvFolketrygden.getMedlemskapsperioder();
        final var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();

        if (fastsattTrygdeavgift != null) {
            medlemskapsperioder
                .stream()
                .map(Medlemskapsperiode::getTrygdeavgift)
                .forEach(Collection::clear);

            final var avgiftsgrunnlag = trygdeavgiftsgrunnlagServiceDeprecated.hentAvgiftsgrunnlag(behandlingsresultatID);

            if (avgiftsgrunnlag.getAvgiftsGrunnlagNorge() != null
                && avgiftsgrunnlag.getAvgiftsGrunnlagNorge().getVurderingTrygdeavgiftNorskInntekt() == NORSK_INNTEKT_TRYGDEAVGIFT_NAV
                && fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd() != null) {
                medlemskapsperioder.stream().filter(Medlemskapsperiode::erInnvilget).forEach(
                    m -> beregnTrygdeavgift(m, avgiftsgrunnlag.getAvgiftsGrunnlagNorge(), fastsattTrygdeavgift.getAvgiftspliktigNorskInntektMnd(), true)
                );
            }

            if (avgiftsgrunnlag.getAvgiftsGrunnlagUtland() != null
                && avgiftsgrunnlag.getAvgiftsGrunnlagUtland().getVurderingTrygdeavgiftUtenlandskInntekt() == UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV
                && fastsattTrygdeavgift.getAvgiftspliktigUtenlandskInntektMnd() != null) {
                medlemskapsperioder.stream().filter(Medlemskapsperiode::erInnvilget).forEach(
                    m -> beregnTrygdeavgift(m, avgiftsgrunnlag.getAvgiftsGrunnlagUtland(), fastsattTrygdeavgift.getAvgiftspliktigUtenlandskInntektMnd(), false)
                );
            }
        }
    }

    private void beregnTrygdeavgift(Medlemskapsperiode medlemskapsperiode,
                                    AvgiftsgrunnlagInfo avgiftsgrunnlag,
                                    long inntektPerMd,
                                    boolean erAvgiftForNorskInntekt) {

        var beregningsresultater = trygdeavgiftConsumer.beregnTrygdeavgift(
            new MelosysTrygdeavgfitBeregningDto(
                avgiftsgrunnlag.betalerArbeidsgiverAvgift(),
                avgiftsgrunnlag.erSkattepliktig(),
                medlemskapsperiode.getDekning(),
                medlemskapsperiode.getBestemmelse(),
                inntektPerMd,
                avgiftsgrunnlag.getSærligAvgiftsgruppe(),
                medlemskapsperiode.getFom(),
                medlemskapsperiode.getTom()
            )
        );

        beregningsresultater.stream().forEach(beregningsresultat ->
            medlemskapsperiode.getTrygdeavgift().add(
                new TrygdeavgiftDeprecated(
                    medlemskapsperiode,
                    beregningsresultat.getMaanedsavgift(),
                    beregningsresultat.getAvgiftssats(),
                    beregningsresultat.getAvgiftskode(),
                    erAvgiftForNorskInntekt,
                    beregningsresultat.getAvgiftPeriodeFom(),
                    beregningsresultat.getAvgiftPeriodeTom()
                )
            )
        );
    }

    @Transactional(readOnly = true)
    public Trygdeavgiftsberegningsresultat hentBeregningsresultat(long behandlingsresultatID) {
        return Trygdeavgiftsberegningsresultat.lag(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID));
    }

    @Transactional(readOnly = true)
    public Optional<Trygdeavgiftsberegningsresultat> finnBeregningsresultat(long behandlingsresultatID) {
        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)
            .map(Trygdeavgiftsberegningsresultat::lag);
    }
}
