package no.nav.melosys.domain.dokument.soeknad;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Land;

/**
 * Opplysninger om opphold i utland
 */
public class OppholdUtland implements HarPeriode {
    public List<Land> oppholdsland = new ArrayList<>();
    public Periode oppholdsPeriode;
    public Boolean studentIEOS;
    public String studentFinansiering;
    public String studentSemester;
    public Land studieLand;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return oppholdsPeriode;
    }
}
