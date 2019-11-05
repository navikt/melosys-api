package no.nav.melosys.repository;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;

public class ProsessinstansAntall {
    private ProsessType prosessType;
    private ProsessSteg prosessSteg;
    private long antall;

    public ProsessinstansAntall(ProsessType prosessType, ProsessSteg prosessSteg, long antall) {
        this.prosessType = prosessType;
        this.prosessSteg = prosessSteg;
        this.antall = antall;
    }

    public ProsessType getProsessType() {
        return prosessType;
    }

    public ProsessSteg getProsessSteg() {
        return prosessSteg;
    }

    public long getAntall() {
        return antall;
    }
}
