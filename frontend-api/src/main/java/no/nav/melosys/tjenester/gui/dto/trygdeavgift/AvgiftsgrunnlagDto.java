package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import no.nav.melosys.domain.avgift.TrygdeavgiftsgrunnlagDeprecated;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;
import no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt;
import no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt;

@Deprecated(since = "Skal fjernes med ny lagring av trygdeavgift: MELOSYS-5827")
public class AvgiftsgrunnlagDto extends OppdaterAvgiftsgrunnlagDto {

    private final Vurderingsutfall_trygdeavgift_norsk_inntekt vurderingTrygdeavgiftNorskInntekt;
    private final Vurderingsutfall_trygdeavgift_utenlandsk_inntekt vurderingTrygdeavgiftUtenlandskInntekt;

    public AvgiftsgrunnlagDto(Loenn_forhold lønnsforhold,
                              AvgiftsgrunnlagInfoDto inntektsinformasjonNorge,
                              AvgiftsgrunnlagInfoDto inntektsinformasjonUtland,
                              Vurderingsutfall_trygdeavgift_norsk_inntekt vurderingTrygdeavgiftNorskInntekt,
                              Vurderingsutfall_trygdeavgift_utenlandsk_inntekt vurderingTrygdeavgiftUtenlandskInntekt) {
        super(lønnsforhold, inntektsinformasjonNorge, inntektsinformasjonUtland);
        this.vurderingTrygdeavgiftNorskInntekt = vurderingTrygdeavgiftNorskInntekt;
        this.vurderingTrygdeavgiftUtenlandskInntekt = vurderingTrygdeavgiftUtenlandskInntekt;
    }

    public Vurderingsutfall_trygdeavgift_norsk_inntekt getVurderingTrygdeavgiftNorskInntekt() {
        return vurderingTrygdeavgiftNorskInntekt;
    }

    public Vurderingsutfall_trygdeavgift_utenlandsk_inntekt getVurderingTrygdeavgiftUtenlandskInntekt() {
        return vurderingTrygdeavgiftUtenlandskInntekt;
    }

    public static AvgiftsgrunnlagDto av(TrygdeavgiftsgrunnlagDeprecated trygdeavgiftsgrunnlagDeprecated) {
        return new AvgiftsgrunnlagDto(
            trygdeavgiftsgrunnlagDeprecated.getLønnsforhold(),
            trygdeavgiftsgrunnlagDeprecated.getAvgiftsGrunnlagNorge() != null ? AvgiftsgrunnlagInfoDto.av(trygdeavgiftsgrunnlagDeprecated.getAvgiftsGrunnlagNorge()) : null,
            trygdeavgiftsgrunnlagDeprecated.getAvgiftsGrunnlagUtland() != null ? AvgiftsgrunnlagInfoDto.av(trygdeavgiftsgrunnlagDeprecated.getAvgiftsGrunnlagUtland()) : null,
            trygdeavgiftsgrunnlagDeprecated.getAvgiftsGrunnlagNorge() != null ? trygdeavgiftsgrunnlagDeprecated.getAvgiftsGrunnlagNorge().getVurderingTrygdeavgiftNorskInntekt() : null,
            trygdeavgiftsgrunnlagDeprecated.getAvgiftsGrunnlagUtland() != null ? trygdeavgiftsgrunnlagDeprecated.getAvgiftsGrunnlagUtland().getVurderingTrygdeavgiftUtenlandskInntekt() : null
        );
    }
}
