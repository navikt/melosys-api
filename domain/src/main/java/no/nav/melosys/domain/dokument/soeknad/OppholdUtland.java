package no.nav.melosys.domain.dokument.soeknad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.felles.Landkode;

/**
 * Opplysninger om opphold i utland
 */
public class OppholdUtland implements HarPeriode{
    public Landkode oppholdsland;
    public Periode oppholdsPeriode;
    public Boolean studentIEOS;
    public String studentFinansiering;
    public String studentSemester;
    public Landkode studieLand;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return oppholdsPeriode;
    }
}
