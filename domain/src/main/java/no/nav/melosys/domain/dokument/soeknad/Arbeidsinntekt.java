package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;

/**
 * Opplysninger om arbeidsinntekt
 */
public class Arbeidsinntekt {

    public int inntektNorskIPerioden;
    public int inntektUtenlandskIPerioden;
    public int inntektNaeringIPerioden;
    public List<String> inntektNaturalYtelser = new ArrayList<>();
    public boolean inntektErInnrapporteringspliktig;
    public boolean inntektTrygdeavgiftBlirTrukket;
}
