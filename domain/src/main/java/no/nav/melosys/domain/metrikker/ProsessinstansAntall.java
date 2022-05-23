package no.nav.melosys.domain.metrikker;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessType;

public class ProsessinstansAntall {
    private ProsessType prosessType;
    private ProsessStatus prosessStatus;
    private long antall;

    public ProsessinstansAntall(ProsessType prosessType, ProsessStatus prosessStatus, long antall) {
        this.prosessType = prosessType;
        this.prosessStatus = prosessStatus;
        this.antall = antall;
    }

    public ProsessType getProsessType() {
        return prosessType;
    }

    public ProsessStatus getProsessStatus() {
        return prosessStatus;
    }

    public long getAntall() {
        return antall;
    }
}
