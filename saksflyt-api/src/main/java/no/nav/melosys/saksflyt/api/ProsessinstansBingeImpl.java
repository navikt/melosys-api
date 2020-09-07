package no.nav.melosys.saksflyt.api;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import no.nav.melosys.domain.saksflyt.Prosessinstans;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansBingeImpl implements ProsessinstansBinge {

    private final Queue<Prosessinstans> prosessinstansKø = new LinkedList<>();

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
