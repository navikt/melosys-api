package no.nav.melosys.saksflyt.impl;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.api.Binge;

/**
 * Implementasjon av det sentrale minnet
 */
@Component
@Scope("singleton")
public class BingeImpl implements Binge {

    private static Logger logger = LoggerFactory.getLogger(BingeImpl.class);

    private HashMap<Long, Prosessinstans> prosessinstanser = new HashMap<>();

    @Override
    public synchronized boolean leggTil(Prosessinstans prosessinstans) {
        if (prosessinstanser.containsKey(prosessinstans.getId())) {
            logger.error("Forsøk på å legge inn prosessinstans som allerede finnes i Bingen. prosessinstansId=%d", prosessinstans.getId());
            return false;
        }
        prosessinstanser.put(prosessinstans.getId(), prosessinstans);
        return true;
    }

    @Override
    public synchronized Prosessinstans hentProsessinstans(long prosessinstansId) {
        return prosessinstanser.get(prosessinstansId);
    }

    @Override
    public synchronized Collection<Prosessinstans> hentProsessinstanser(Predicate<Prosessinstans> predikat) {
        return prosessinstanser.values().stream().filter(predikat).collect(Collectors.toList());
    }

    @Override
    public synchronized List<Prosessinstans> hentProsessinstanser(Predicate<Prosessinstans> predikat, Comparator<Prosessinstans> rekkefølge) {
        return prosessinstanser.values().stream().filter(predikat).sorted(rekkefølge).collect(Collectors.toList());
    }

    @Override
    public synchronized Prosessinstans fjernProsessinstans(long prosessinstansId) {
        Prosessinstans prosessinstans = prosessinstanser.remove(prosessinstansId);
        if (prosessinstans == null) {
            logger.error("Forsøk på å fjerne prosessinstans som ikke finnes i bingen. prosessinstansId=%d", prosessinstansId);
        }
        return prosessinstans;
    }

    @Override
    public synchronized Prosessinstans fjernFørsteProsessinstans(Predicate<Prosessinstans> predikat) {
        Prosessinstans prosessinstans = prosessinstanser.values().stream().filter(predikat).findFirst().orElse(null);
        if (prosessinstans != null) {
            prosessinstanser.remove(prosessinstans.getId());
        }
        return prosessinstans;
    }

    @Override
    public synchronized Prosessinstans fjernFørsteProsessinstans(Predicate<Prosessinstans> predikat, Comparator<Prosessinstans> rekkefølge) {
        Prosessinstans prosessinstans = prosessinstanser.values().stream().filter(predikat).sorted(rekkefølge).findFirst().orElse(null);
        if (prosessinstans != null) {
            prosessinstanser.remove(prosessinstans.getId());
        }
        return prosessinstans;
    }

}
