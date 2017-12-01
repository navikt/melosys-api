package no.nav.melosys.domain.dokument.soeknad;

import java.util.List;

/**
 * Opplysninger om arbeidsinntekt
 */
public class Arbeidsinntekt {

    public Integer inntektNorskIPerioden;
    public Integer inntektUtenlandskIPerioden;
    public Integer inntektNaeringIPerioden;
    public List<String> inntektNaturalYtelser;
    public Boolean inntektErInnrapporteringspliktig;
    public Boolean inntektTrygdeavgiftBlirTrukket;
}
