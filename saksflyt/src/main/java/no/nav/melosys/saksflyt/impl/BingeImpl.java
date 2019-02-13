package no.nav.melosys.saksflyt.impl;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Implementasjon av det sentrale minnet
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class BingeImpl implements Binge {

    private static Logger logger = LoggerFactory.getLogger(BingeImpl.class);

    private HashMap<UUID, Prosessinstans> prosessinstanser = new HashMap<>();

    @Override
    public synchronized boolean leggTil(Prosessinstans prosessinstans) {
        if (prosessinstans.getId() == null) {
            throw new IllegalStateException("Forsøk på å legge inn prosessinstans uten ID i Bingen!");
        }
        if (prosessinstanser.containsKey(prosessinstans.getId())) {
            logger.error("Forsøk på å legge inn prosessinstans som allerede finnes i Bingen. prosessinstansId={}", prosessinstans.getId());
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
            logger.error("Forsøk på å fjerne prosessinstans som ikke finnes i bingen. prosessinstansId={}", prosessinstansId);
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
        Prosessinstans prosessinstans = prosessinstanser.values().stream().filter(predikat).min(rekkefølge).orElse(null);
        if (prosessinstans != null) {
            prosessinstanser.remove(prosessinstans.getId());
        }
        return prosessinstans;
    }

}
