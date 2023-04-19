package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
public class AvgiftsgrunnlagInfoUtland extends AvgiftsgrunnlagInfo {

    private final Vurderingsutfall_trygdeavgift_utenlandsk_inntekt vurderingTrygdeavgiftUtenlandskInntekt;

    public AvgiftsgrunnlagInfoUtland(boolean erSkattepliktig,
                                     boolean betalerArbeidsgiverAvgift,
                                     Saerligeavgiftsgrupper særligAvgiftsgruppe,
                                     Vurderingsutfall_trygdeavgift_utenlandsk_inntekt vurderingTrygdeavgiftUtenlandskInntekt) {
        super(erSkattepliktig, betalerArbeidsgiverAvgift, særligAvgiftsgruppe);
        this.vurderingTrygdeavgiftUtenlandskInntekt = vurderingTrygdeavgiftUtenlandskInntekt;
    }

    public Vurderingsutfall_trygdeavgift_utenlandsk_inntekt getVurderingTrygdeavgiftUtenlandskInntekt() {
        return vurderingTrygdeavgiftUtenlandskInntekt;
    }
}
