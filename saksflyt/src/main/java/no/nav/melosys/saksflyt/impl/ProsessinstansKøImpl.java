package no.nav.melosys.saksflyt.impl;

import java.util.*;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.api.ProsessinstansKø;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansKøImpl implements ProsessinstansKø {

    private final Queue<Prosessinstans> prosessinstansKø = new LinkedList<>();

    @Override
    public synchronized Collection<Prosessinstans> hentProsessinstanser() {
        return new ArrayList<>(prosessinstansKø);
    }

    @Override
    public synchronized boolean leggTil(Prosessinstans prosessinstans) {
        if (prosessinstansKø.contains(prosessinstans)) {
            return false;
        }

        return prosessinstansKø.add(prosessinstans);
    }

    @Override
    public synchronized Optional<Prosessinstans> plukkNeste() {
        return !prosessinstansKø.isEmpty()
            ? Optional.of(prosessinstansKø.remove())
            : Optional.empty();
    }
}
