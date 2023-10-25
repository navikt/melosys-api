package no.nav.melosys.saksflyt.prosessflyt;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.ProsessType;

public class ProsessFlyt {

    private final ProsessType prosessType;
    private final List<ProsessSteg> stegListe;

    ProsessFlyt(ProsessType prosessType, ProsessSteg... prosessSteg) {
        ArrayList<ProsessSteg> prosessStegListe = new ArrayList<>();

        for (ProsessSteg steg : prosessSteg) {
            if (prosessStegListe.contains(steg)) {
                throw new IllegalArgumentException("Prosessteg " + steg + " er definert to eller flere ganger!");
            }
            prosessStegListe.add(steg);
        }

        this.prosessType = prosessType;
        this.stegListe = List.copyOf(prosessStegListe);
    }

    @Nullable
    public ProsessSteg nesteSteg(ProsessSteg forrigeSteg) {
        Iterator <ProsessSteg> iter = stegListe.iterator();

        if (forrigeSteg == null) return iter.next();

        while (iter.hasNext()) {
            ProsessSteg s = iter.next();
            if (s == forrigeSteg) return iter.hasNext() ? iter.next() : null;
        }

        throw new IllegalArgumentException("Forrige steg " + forrigeSteg + " er ikke gyldig for prosesstype " + prosessType);
    }
}
