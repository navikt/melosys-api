package no.nav.melosys.saksflyt.impl;

import java.util.*;
import java.util.function.Predicate;

import no.nav.melosys.domain.ProsessSteg;
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
    private HashMap<UUID, Prosessinstans> aktiveProsessinstanser = new HashMap<>();

    @Override
    public synchronized boolean leggTil(Prosessinstans prosessinstans) {
        if (prosessinstans.getId() == null) {
            throw new IllegalStateException("Forsøk på å legge inn prosessinstans uten ID i Bingen!");
        }
        if (ProsessSteg.FEILET_MASKINELT.equals(prosessinstans.getSteg()) || ProsessSteg.FERDIG.equals(prosessinstans.getSteg())) {
            logger.warn("Forsøk på å legge inn prosessinstans {} med ugyldig steg {} ", prosessinstans.getId(), prosessinstans.getSteg());
            return false;
        }
        if (prosessinstanser.containsKey(prosessinstans.getId()) ||
            aktiveProsessinstanser.containsKey(prosessinstans.getId())) {
            logger.warn("Forsøk på å legge inn prosessinstans som allerede finnes i Bingen. prosessinstansId={}", prosessinstans.getId());
            return false;
        }
        prosessinstanser.put(prosessinstans.getId(), prosessinstans);
        return true;
    }

    @Override
    public synchronized Collection<Prosessinstans> hentProsessinstanser() {
        List<Prosessinstans> alleProsessinstanser = new ArrayList<>();
        alleProsessinstanser.addAll(prosessinstanser.values());
        alleProsessinstanser.addAll(aktiveProsessinstanser.values());
        return alleProsessinstanser;
    }

    @Override
    public synchronized Prosessinstans hentOgSettProsessinstansTilAktiv(Predicate<Prosessinstans> predikat) {
        Prosessinstans prosessinstans = prosessinstanser.values().stream()
            .filter(predikat)
            .sorted(Utils.eldsteFørst())
            .findFirst().orElse(null);

        if (prosessinstans != null) {
            UUID uuid = prosessinstans.getId();
            aktiveProsessinstanser.put(uuid, prosessinstans);
            prosessinstanser.remove(uuid);
        }
        return prosessinstans;
    }

    @Override
    public synchronized void fjernFraAktiveProsessinstanser(Prosessinstans prosessinstans) {
        aktiveProsessinstanser.remove(prosessinstans.getId());
    }
}
