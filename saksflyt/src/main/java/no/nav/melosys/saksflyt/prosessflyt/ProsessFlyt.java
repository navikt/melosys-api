package no.nav.melosys.saksflyt.prosessflyt;


import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;

public class ProsessFlyt {

    private final ProsessType prosessType;
    private final List<ProsessSteg> steg;

    ProsessFlyt(ProsessType prosessType, List<ProsessSteg> steg) {
        this.prosessType = prosessType;
        this.steg = steg;
    }

    @Nullable
    public ProsessSteg nesteSteg(ProsessSteg forrigeSteg) {
        Iterator <ProsessSteg> iter = steg.iterator();

        if (forrigeSteg == null) return iter.next();

        while (iter.hasNext()) {
            ProsessSteg s = iter.next();
            if (s == forrigeSteg) return iter.hasNext() ? iter.next() : null;
        }

        throw new IllegalArgumentException("Forrige steg " + forrigeSteg + " er ikke gyldig for prosesstype " + prosessType);
    }
}
