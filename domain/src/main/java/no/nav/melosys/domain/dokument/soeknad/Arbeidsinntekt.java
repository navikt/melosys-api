package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;

/**
 * Opplysninger om arbeidsinntekt
 */
public class Arbeidsinntekt {

    public Integer inntektNorskIPerioden;
    public Integer inntektUtenlandskIPerioden;
    public Integer inntektNaeringIPerioden;
    public List<String> inntektNaturalYtelser = new ArrayList<>();
    public Boolean inntektErInnrapporteringspliktig;
    public Boolean inntektTrygdeavgiftBlirTrukket;
}
