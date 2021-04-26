package no.nav.melosys.service.avgift;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfo;
import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsberegningRequest;
import no.nav.melosys.domain.avgift.Trygdeavgift;
import no.nav.melosys.domain.avgift.Trygdeavgiftsberegningsresultat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer;
import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningDto;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV;

@Service
public class TrygdeavgiftsberegningService {

    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    private final TrygdeavgiftConsumer trygdeavgiftConsumer;

    public TrygdeavgiftsberegningService(TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                         MedlemAvFolketrygdenService medlemAvFolketrygdenService, TrygdeavgiftConsumer trygdeavgiftConsumer) {
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
        this.trygdeavgiftConsumer = trygdeavgiftConsumer;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void oppdaterBeregningsgrunnlag(long behandlingsresultatID, OppdaterTrygdeavgiftsberegningRequest request) throws FunksjonellException {
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

    @Transactional(rollbackFor = MelosysException.class)
    public void beregnAvgift(long behandlingsresultatID) throws IkkeFunnetException {
        final var medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID);
        final var medlemskapsperioder = medlemAvFolketrygden.getMedlemskapsperioder();
        final var fastsattTrygdeavgift = medlemAvFolketrygden.getFastsattTrygdeavgift();

        if (fastsattTrygdeavgift != null) {
            medlemskapsperioder
                .stream()
                .map(Medlemskapsperiode::getTrygdeavgift)
                .forEach(Collection::clear);

            final var avgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingsresultatID);

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
        var beregningsresultat = trygdeavgiftConsumer.beregnTrygdeavgift(
            new MelosysTrygdeavgfitBeregningDto(
                avgiftsgrunnlag.betalerArbeidsgiverAvgift(),
                avgiftsgrunnlag.erSkattepliktig(),
                medlemskapsperiode.getDekning(),
                medlemskapsperiode.getBestemmelse(),
                inntektPerMd,
                LocalDate.now(),
                avgiftsgrunnlag.getSærligAvgiftsgruppe()
            )
        );

        medlemskapsperiode.getTrygdeavgift().add(
            new Trygdeavgift(
                medlemskapsperiode,
                beregningsresultat.getMaanedsavgift(),
                beregningsresultat.getAvgiftssats(),
                beregningsresultat.getAvgiftskode(),
                erAvgiftForNorskInntekt
            )
        );
    }

    @Transactional(readOnly = true)
    public Trygdeavgiftsberegningsresultat hentBeregningsresultat(long behandlingsresultatID) throws IkkeFunnetException {
        return Trygdeavgiftsberegningsresultat.lag(medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID));
    }

    @Transactional(readOnly = true)
    public Optional<Trygdeavgiftsberegningsresultat> finnBeregningsresultat(long behandlingsresultatID) {
        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)
            .map(Trygdeavgiftsberegningsresultat::lag);
    }
}
