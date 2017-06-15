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

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.saksflyt.api.Binge;

/**
 * Implementasjon av det sentrale minnet
 */
@Component
@Scope("singleton")
public class BingeImpl implements Binge {

    private static Logger logger = LoggerFactory.getLogger(BingeImpl.class);

    private HashMap<Long, Behandling> behandlinger = new HashMap<>();

    @Override
    public synchronized boolean leggTil(Behandling behandling) {
        if (behandlinger.containsKey(behandling.getBehandlingsId())) {
            logger.error("Forsøk på å legge inn behandling som allerede finnes i Bingen. behandlingsid=%d", behandling.getBehandlingsId());
            return false;
        }
        behandlinger.put(behandling.getBehandlingsId(), behandling);
        return true;
    }

    @Override
    public synchronized Behandling hentBehandling(long behandlingsId) {
        return behandlinger.get(behandlingsId);
    }

    @Override
    public synchronized Collection<Behandling> hentBehandlinger(Predicate<Behandling> predikat) {
        return behandlinger.values().stream().filter(predikat).collect(Collectors.toList());
    }

    @Override
    public synchronized List<Behandling> hentBehandlinger(Predicate<Behandling> predikat, Comparator<Behandling> rekkefølge) {
        return behandlinger.values().stream().filter(predikat).sorted(rekkefølge).collect(Collectors.toList());
    }

    @Override
    public synchronized Behandling fjernBehandling(long behandlingsId) {
        Behandling behandling = behandlinger.remove(behandlingsId);
        if (behandling == null) {
            logger.error("Forsøk på å fjerne behandling som ikke finnes i bingen. behandlingsid=%d", behandlingsId);
        }
        return behandling;
    }

    @Override
    public synchronized Behandling fjernFørsteBehandling(Predicate<Behandling> predikat) {
        Behandling behandling = behandlinger.values().stream().filter(predikat).findFirst().orElse(null);
        if (behandling != null) {
            behandlinger.remove(behandling.getBehandlingsId());
        }
        return behandling;
    }

    @Override
    public synchronized Behandling fjernFørsteBehandling(Predicate<Behandling> predikat, Comparator<Behandling> rekkefølge) {
        Behandling behandling = behandlinger.values().stream().filter(predikat).sorted(rekkefølge).findFirst().orElse(null);
        if (behandling != null) {
            behandlinger.remove(behandling.getBehandlingsId());
        }
        return behandling;
    }

}
