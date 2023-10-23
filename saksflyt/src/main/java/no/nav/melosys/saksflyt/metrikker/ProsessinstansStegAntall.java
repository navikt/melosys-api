package no.nav.melosys.saksflyt.metrikker;

import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.ProsessType;

public class ProsessinstansStegAntall {
    private ProsessSteg sistFullfortSteg;
    private ProsessType prosessType;
    private ProsessStatus prosessStatus;
    private long antall;

    public ProsessinstansStegAntall(ProsessSteg prosessSteg, ProsessType prosessType, ProsessStatus prosessStatus, long antall) {
        this.sistFullfortSteg = prosessSteg;
        this.prosessType = prosessType;
        this.prosessStatus = prosessStatus;
        this.antall = antall;
    }

    public ProsessSteg getSistFullfortSteg() {
        return sistFullfortSteg;
    }

    public ProsessStatus getProsessStatus() {
        return prosessStatus;
    }

    public long getAntall() {
        return antall;
    }

    public ProsessType getProsessType() {
        return prosessType;
    }
}
