package no.nav.melosys.domain.mottatteopplysninger.data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;

/**
 * Opplysninger om opphold i utland
 */
public class OppholdUtland implements HarPeriode {
    public List<String> oppholdslandkoder = new ArrayList<>();
    public Periode oppholdsPeriode = new Periode();
    public String studentFinansieringKode;
    public String studentSemester;
    public Boolean ektefelleEllerBarnINorge;

    @Override
    @JsonIgnore
    public ErPeriode getPeriode() {
        return oppholdsPeriode;
    }
}
