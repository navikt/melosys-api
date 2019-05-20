package no.nav.melosys.domain.dokument.soeknad;

/**
 * Opplysninger om arbeidsinntekt
 */
public class Arbeidsinntekt {
    public Integer inntektNorskIPerioden;
    public Integer inntektUtenlandskIPerioden;
    public Integer inntektNaeringIPerioden;
    public ArbeidsinntektNaturalytelser inntektNaturalytelser;
    public Boolean inntektErInnrapporteringspliktig;
    public Boolean inntektTrygdeavgiftBlirTrukket;
}
