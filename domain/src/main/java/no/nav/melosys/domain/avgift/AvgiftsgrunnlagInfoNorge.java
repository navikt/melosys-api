package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
public class AvgiftsgrunnlagInfoNorge extends AvgiftsgrunnlagInfo {

    private final Vurderingsutfall_trygdeavgift_norsk_inntekt vurderingTrygdeavgiftNorskInntekt;

    public AvgiftsgrunnlagInfoNorge(boolean erSkattepliktig,
                                    boolean betalerArbeidsgiverAvgift,
                                    Saerligeavgiftsgrupper særligAvgiftsgruppe,
                                    Vurderingsutfall_trygdeavgift_norsk_inntekt vurderingTrygdeavgiftNorskInntekt) {
        super(erSkattepliktig, betalerArbeidsgiverAvgift, særligAvgiftsgruppe);
        this.vurderingTrygdeavgiftNorskInntekt = vurderingTrygdeavgiftNorskInntekt;
    }

    public Vurderingsutfall_trygdeavgift_norsk_inntekt getVurderingTrygdeavgiftNorskInntekt() {
        return vurderingTrygdeavgiftNorskInntekt;
    }
}
