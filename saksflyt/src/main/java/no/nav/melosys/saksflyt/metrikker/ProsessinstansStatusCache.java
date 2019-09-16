package no.nav.melosys.saksflyt.metrikker;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.repository.ProsessinstansAntall;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansStatusCache {
    private ProsessinstansRepository prosessinstansRepository;
    private final long MAX_DATA_LEVETID_MS;

    private Map<Pair<ProsessType, ProsessSteg>, Long> antallPerTypeOgSteg;
    private long sistLestTidspunkt = 0;

    private static final EnumSet<ProsessSteg> STEG_FEILET = EnumSet.of(ProsessSteg.FEILET_MASKINELT);

    @Autowired
    public ProsessinstansStatusCache(ProsessinstansRepository prosessinstansRepository,
                                     @Value("${melosys.prosesser.status.cache.levetid:1000}") long millisLevetidICache) {
        this.prosessinstansRepository = prosessinstansRepository;
        this.MAX_DATA_LEVETID_MS = millisLevetidICache;
    }

    double antallProsessinstanserFeilet(ProsessType type) {
        return (double) antallProsessinstanserMedTypeSteg(type, STEG_FEILET);
    }

    private long antallProsessinstanserMedTypeSteg(ProsessType prosessType, EnumSet<ProsessSteg> steg) {
        oppfriskCacheHvisUtløpt();

        long sumAntall = 0;
        for (ProsessSteg prosessSteg : steg) {
            Pair<ProsessType, ProsessSteg> prosessTypeOgSteg = Pair.of(prosessType, prosessSteg);
            Long antall = antallPerTypeOgSteg.get(prosessTypeOgSteg);
            if (antall != null) {
                sumAntall = sumAntall + antall;
            }
        }
        return sumAntall;
    }

    private void oppfriskCacheHvisUtløpt() {
        long nå = System.currentTimeMillis();
        long alderMs = nå - sistLestTidspunkt;
        if (alderMs >= MAX_DATA_LEVETID_MS) {
            oppfriskCache();
            sistLestTidspunkt = System.currentTimeMillis();
        }
    }

    private void oppfriskCache() {
        List<ProsessinstansAntall> prosessinstansMetrikker = prosessinstansRepository.antallAktiveOgFeiletPerTypeOgSteg();

        antallPerTypeOgSteg = new HashMap<>();
        for (ProsessinstansAntall prosessinstansAntall : prosessinstansMetrikker) {
            Pair<ProsessType, ProsessSteg> typeOgStatus = Pair.of(prosessinstansAntall.getProsessType(), prosessinstansAntall.getProsessSteg());
            antallPerTypeOgSteg.put(typeOgStatus, prosessinstansAntall.getAntall());
        }
    }
}
