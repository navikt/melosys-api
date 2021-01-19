package no.nav.melosys.domain.behandlingsgrunnlag.data;

public class Arbeidsinntekt {
    public Integer inntektNorskIPerioden;
    public Integer inntektUtenlandskIPerioden;
    public Integer inntektNaeringIPerioden;
    public ArbeidsinntektNaturalytelser inntektNaturalytelser;
    public Boolean inntektErInnrapporteringspliktig;
    public Boolean inntektTrygdeavgiftBlirTrukket;
}
