package no.nav.melosys.saksflyt.impl.binge;

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

import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.api.Sak;

/**
 * Implementasjon av det sentrale minnet
 */
@Component
@Scope("singleton")
public class BingeImpl implements Binge {
    
    private static Logger logger = LoggerFactory.getLogger(BingeImpl.class);
    
    private HashMap<Long, Sak> saker = new HashMap<>();

    @Override
    public synchronized boolean leggTilSak(Sak sak) {
        if (saker.containsKey(sak.getSaksId())) {
            logger.error("Forsøk på å legge inn sak som allerede er finnes Bingen. saksid=%d", sak.getSaksId());
            return false;
        }
        saker.put(sak.getSaksId(), sak);
        return true;
    }

    @Override
    public synchronized Sak hentSak(long saksId) {
        return saker.get(saksId);
    }

    @Override
    public synchronized Collection<Sak> hentSaker(Predicate<Sak> predikat) {
        return saker.values().stream().filter(predikat).collect(Collectors.toList());
    }

    @Override
    public synchronized List<Sak> hentSaker(Predicate<Sak> predikat, Comparator<Sak> rekkefølge) {
        return saker.values().stream().filter(predikat).sorted(rekkefølge).collect(Collectors.toList());
    }

    @Override
    public synchronized Sak fjernSak(long saksId) {
        Sak sak = saker.remove(saksId);
        if (sak == null) {
            logger.error("Forsøk på å fjerne sak som ikke finnes i bingen. saksid=%d", saksId);
        }
        return sak;
    }

    @Override
    public synchronized Sak fjernFørsteSak(Predicate<Sak> predikat, Comparator<Sak> rekkefølge) {
        Sak sak = saker.values().stream().filter(predikat).sorted(rekkefølge).findFirst().orElse(null);
        if (sak != null) {
            saker.remove(sak.getSaksId());
        }
        return sak;
    }

}
