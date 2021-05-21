package no.nav.melosys.saksflyt.metrikker;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.repository.ProsessinstansAntall;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansStatusCache {
    private final ProsessinstansRepository prosessinstansRepository;
    private final long MAX_DATA_LEVETID_MS;

    private Map<Pair<ProsessType, ProsessStatus>, Long> antallPerTypeOgStatus;
    private long sistLestTidspunkt = 0;

    private static final EnumSet<ProsessType> PROSESS_TYPER = EnumSet.allOf(ProsessType.class);
    private static final EnumSet<ProsessStatus> STATUS_FEILET = EnumSet.of(ProsessStatus.FEILET);

    @Autowired
    public ProsessinstansStatusCache(ProsessinstansRepository prosessinstansRepository,
                                     @Value("${melosys.prosesser.status.cache.levetid:1000}") long millisLevetidICache) {
        this.prosessinstansRepository = prosessinstansRepository;
        this.MAX_DATA_LEVETID_MS = millisLevetidICache;
    }

    double antallProsessinstanserFeilet(ProsessType type) {
        return (double) antallProsessinstanserMedTypeOgStatus(type, STATUS_FEILET);
    }

    private long antallProsessinstanserMedTypeOgStatus(ProsessType prosessType, EnumSet<ProsessStatus> statuser) {
        oppfriskCacheHvisUtløpt();

        long sumAntall = 0;
        for (ProsessStatus prosessStatus : statuser) {
            Pair<ProsessType, ProsessStatus> prosessTypeOgSteg = Pair.of(prosessType, prosessStatus);
            Long antall = antallPerTypeOgStatus.get(prosessTypeOgSteg);
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
        Collection<ProsessinstansAntall> prosessinstansMetrikker = prosessinstansRepository.
            antallAktiveOgFeiletPerTypeOgStatus(PROSESS_TYPER);

        antallPerTypeOgStatus = new HashMap<>();
        for (ProsessinstansAntall prosessinstansAntall : prosessinstansMetrikker) {
            Pair<ProsessType, ProsessStatus> typeOgStatus = Pair.of(prosessinstansAntall.getProsessType(), prosessinstansAntall.getProsessStatus());
            antallPerTypeOgStatus.put(typeOgStatus, prosessinstansAntall.getAntall());
        }
    }
}
