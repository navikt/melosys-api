package no.nav.melosys.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.api.Binge;

class BingeTestImpl implements Binge {

    private final HashMap<Long, Prosessinstans> prosessinstanser = new HashMap<>();

    @Override
    public boolean leggTil(Prosessinstans prosessinstans) {
        prosessinstanser.put(prosessinstans.getId(), prosessinstans);
        return true;
    }

    @Override
    public Prosessinstans hentProsessinstans(long prosessinstansId) {
        return prosessinstanser.get(prosessinstansId);
    }

    @Override
    public Collection<Prosessinstans> hentProsessinstanser(Predicate<Prosessinstans> predikat) {
        return null;
    }

    @Override
    public List<Prosessinstans> hentProsessinstanser(Predicate<Prosessinstans> predikat, Comparator<Prosessinstans> rekkefølge) {
        return null;
    }

    @Override
    public Prosessinstans fjernProsessinstans(long prosessinstansId) {
        return null;
    }

    @Override
    public Prosessinstans fjernFørsteProsessinstans(Predicate<Prosessinstans> predikat) {
        return null;
    }

    @Override
    public Prosessinstans fjernFørsteProsessinstans(Predicate<Prosessinstans> predikat, Comparator<Prosessinstans> rekkefølge) {
        return null;
    }
}
