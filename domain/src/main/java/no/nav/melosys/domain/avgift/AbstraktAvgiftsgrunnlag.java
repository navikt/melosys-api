package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Loenn_forhold;

public abstract class AbstraktAvgiftsgrunnlag {
    protected final Loenn_forhold lønnsforhold;
    protected final AvgiftsgrunnlagInfo avgiftsGrunnlagNorge;
    protected final AvgiftsgrunnlagInfo avgiftsGrunnlagUtland;

    public AbstraktAvgiftsgrunnlag(Loenn_forhold lønnsforhold,
                                   AvgiftsgrunnlagInfo avgiftsGrunnlagNorge,
                                   AvgiftsgrunnlagInfo avgiftsGrunnlagUtland) {
        this.lønnsforhold = lønnsforhold;
        this.avgiftsGrunnlagNorge = avgiftsGrunnlagNorge;
        this.avgiftsGrunnlagUtland = avgiftsGrunnlagUtland;
    }

    public Loenn_forhold getLønnsforhold() {
        return lønnsforhold;
    }

    public AvgiftsgrunnlagInfo getAvgiftsGrunnlagNorge() {
        return avgiftsGrunnlagNorge;
    }

    public AvgiftsgrunnlagInfo getAvgiftsGrunnlagUtland() {
        return avgiftsGrunnlagUtland;
    }
}
