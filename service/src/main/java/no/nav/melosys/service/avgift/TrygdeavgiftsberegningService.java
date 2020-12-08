package no.nav.melosys.service.avgift;

import java.time.LocalDate;
import java.util.Collection;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfo;
import no.nav.melosys.domain.avgift.OppdaterTrygdeavgiftsberegningRequest;
import no.nav.melosys.domain.avgift.Trygdeavgift;
import no.nav.melosys.domain.avgift.Trygdeavgiftsberegningsresultat;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer;
import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningDto;
import no.nav.melosys.repository.MedlemAvFolketrygdenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt.NORSK_INNTEKT_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV;
import static no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt.UTENLANDSK_INNTEKT_TRYGDEAVGIFT_NAV;

@Service
public class TrygdeavgiftsberegningService {

    private final TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService;
    private final MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository;
    private final TrygdeavgiftConsumer trygdeavgiftConsumer;

    public TrygdeavgiftsberegningService(TrygdeavgiftsgrunnlagService trygdeavgiftsgrunnlagService,
                                         MedlemAvFolketrygdenRepository medlemAvFolketrygdenRepository,
                                         TrygdeavgiftConsumer trygdeavgiftConsumer) {
        this.trygdeavgiftsgrunnlagService = trygdeavgiftsgrunnlagService;
        this.medlemAvFolketrygdenRepository = medlemAvFolketrygdenRepository;
        this.trygdeavgiftConsumer = trygdeavgiftConsumer;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void oppdaterBeregningsgrunnlag(long behandlingsresultatID, OppdaterTrygdeavgiftsberegningRequest request) throws FunksjonellException {
        final var medlemAvFolketrygden = hentMedlemAvFolketrygden(behandlingsresultatID);

        if (request.getAvgiftspliktigLønnNorge() != null && medlemAvFolketrygden.getVurderingTrygdeavgiftNorskInntekt() == NORSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV) {
            throw new FunksjonellException("Skal ikke betales trygdeavgift for norsk inntekt");
        }
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigNorskInntektMnd(request.getAvgiftspliktigLønnNorge());

        if (request.getAvgiftspliktigLønnUtland() != null && medlemAvFolketrygden.getVurderingTrygdeavgiftUtenlandskInntekt() == UTENLANDSK_INNTEKT_INGEN_TRYGDEAVGIFT_NAV) {
            throw new FunksjonellException("Skal ikke betales trygdeavgift for utenlandsk inntekt");
        }
        medlemAvFolketrygden.getFastsattTrygdeavgift().setAvgiftspliktigUtenlandskInntektMnd(request.getAvgiftspliktigLønnUtland());
        beregnAvgift(behandlingsresultatID);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void beregnAvgift(long behandlingsresultatID) throws IkkeFunnetException {
        final var medlemAvFolketrygden = hentMedlemAvFolketrygden(behandlingsresultatID);
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
                avgiftsgrunnlag.getBetalerArbeidsgiverAvgift(),
                avgiftsgrunnlag.getErSkattepliktig(),
                medlemskapsperiode.getTrygdedekning(),
                medlemskapsperiode.getBestemmelse(),
                inntektPerMd,
                LocalDate.now(),
                avgiftsgrunnlag.getSærligAvgiftsgruppe()
            )
        );

        medlemskapsperiode.getTrygdeavgift().add(
            new Trygdeavgift(
                medlemskapsperiode,
                beregningsresultat.getMaanedsavgift().doubleValue(),
                beregningsresultat.getAvgiftssats().doubleValue(),
                beregningsresultat.getAvgiftskode(),
                erAvgiftForNorskInntekt
            )
        );
    }

    @Transactional(readOnly = true)
    public Trygdeavgiftsberegningsresultat hentBeregningsresultat(long behandlingsresultatID) throws IkkeFunnetException {
        return Trygdeavgiftsberegningsresultat.lag(hentMedlemAvFolketrygden(behandlingsresultatID));
    }

    private MedlemAvFolketrygden hentMedlemAvFolketrygden(long behandlingsresultatID) throws IkkeFunnetException {
        return medlemAvFolketrygdenRepository.findByBehandlingsresultatId(behandlingsresultatID)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke medlemAvFolketrygden for behandlingsresultatID " + behandlingsresultatID));
    }
}
