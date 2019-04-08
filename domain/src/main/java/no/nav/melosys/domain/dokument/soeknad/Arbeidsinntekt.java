package no.nav.melosys.domain.dokument.soeknad;

/**
 * Opplysninger om arbeidsinntekt
 */
public class Arbeidsinntekt {
    public int inntektNorskIPerioden;
    public int inntektUtenlandskIPerioden;
    public int inntektNaeringIPerioden;
    public ArbeidsinntektNaturalytelser inntektNaturalytelser;
    public Boolean inntektErInnrapporteringspliktig;
    public Boolean inntektTrygdeavgiftBlirTrukket;
}
