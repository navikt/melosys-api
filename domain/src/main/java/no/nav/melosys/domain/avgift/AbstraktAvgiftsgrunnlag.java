package no.nav.melosys.domain.avgift;

import no.nav.melosys.domain.kodeverk.Loenn_forhold;

public abstract class AbstraktAvgiftsgrunnlag<N extends AvgiftsgrunnlagInfo, U extends AvgiftsgrunnlagInfo> {
    protected final Loenn_forhold lønnsforhold;
    protected final N avgiftsGrunnlagNorge;
    protected final U avgiftsGrunnlagUtland;

    public AbstraktAvgiftsgrunnlag(Loenn_forhold lønnsforhold,
                                   N avgiftsGrunnlagNorge,
                                   U avgiftsGrunnlagUtland) {
        this.lønnsforhold = lønnsforhold;
        this.avgiftsGrunnlagNorge = avgiftsGrunnlagNorge;
        this.avgiftsGrunnlagUtland = avgiftsGrunnlagUtland;
    }

    public Loenn_forhold getLønnsforhold() {
        return lønnsforhold;
    }

    public N getAvgiftsGrunnlagNorge() {
        return avgiftsGrunnlagNorge;
    }

    public U getAvgiftsGrunnlagUtland() {
        return avgiftsGrunnlagUtland;
    }

    public static boolean harLønnsforholdINorge(Loenn_forhold lønnsforhold) {
        return lønnsforhold == Loenn_forhold.DELT_LØNN || lønnsforhold == Loenn_forhold.LØNN_FRA_NORGE;
    }

    public static boolean harLønnsforholdIUtlandet(Loenn_forhold lønnsforhold) {
        return lønnsforhold == Loenn_forhold.DELT_LØNN || lønnsforhold == Loenn_forhold.LØNN_FRA_UTLANDET;
    }
}
