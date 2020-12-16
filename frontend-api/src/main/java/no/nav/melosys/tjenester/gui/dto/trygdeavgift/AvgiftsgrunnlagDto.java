package no.nav.melosys.tjenester.gui.dto.trygdeavgift;

import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag;
import no.nav.melosys.domain.kodeverk.Loenn_forhold;
import no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_norsk_inntekt;
import no.nav.melosys.domain.kodeverk.Vurderingsutfall_trygdeavgift_utenlandsk_inntekt;

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

    public static AvgiftsgrunnlagDto av(Trygdeavgiftsgrunnlag trygdeavgiftsgrunnlag) {
        return new AvgiftsgrunnlagDto(
            trygdeavgiftsgrunnlag.getLønnsforhold(),
            trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge() != null ? AvgiftsgrunnlagInfoDto.av(trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge()) : null,
            trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland() != null ? AvgiftsgrunnlagInfoDto.av(trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge()) : null,
            trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge() != null ? trygdeavgiftsgrunnlag.getAvgiftsGrunnlagNorge().getVurderingTrygdeavgiftNorskInntekt() : null,
            trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland() != null ? trygdeavgiftsgrunnlag.getAvgiftsGrunnlagUtland().getVurderingTrygdeavgiftUtenlandskInntekt() : null
        );
    }
}
