package no.nav.melosys.saksflyt.metrikker;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.metrikker.ProsessinstansAntall;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.domain.metrikker.ProsessinstansStegAntall;
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytDefinisjon;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansStatusCache {
    private final ProsessinstansRepository prosessinstansRepository;
    private final long maksDataLevetidMs;

    private Map<Pair<ProsessType, ProsessStatus>, Long> antallPerTypeOgStatus;
    private Map<Pair<ProsessSteg, ProsessStatus>, Long> antallPerStegOgStatus;
    private long sistLestTidspunkt = 0;

    private static final EnumSet<ProsessType> PROSESS_TYPER = EnumSet.allOf(ProsessType.class);
    private static final EnumSet<ProsessSteg> PROSESS_STEG = EnumSet.allOf(ProsessSteg.class);
    private static final EnumSet<ProsessStatus> STATUS_FEILET = EnumSet.of(ProsessStatus.FEILET);

    public ProsessinstansStatusCache(ProsessinstansRepository prosessinstansRepository,
                                     @Value("${melosys.prosesser.status.cache.levetid:1000}") long millisLevetidICache) {
        this.prosessinstansRepository = prosessinstansRepository;
        this.maksDataLevetidMs = millisLevetidICache;
    }

    double antallProsessinstanserFeiletPåType(ProsessType type) {
        return antallProsessinstanserMedTypeOgStatus(type, STATUS_FEILET);
    }

    double antallProsessinstanserFeiletPåSteg(ProsessSteg prosessSteg) {
        return antallProsessinstanserMedStegOgStatus(prosessSteg, STATUS_FEILET);
    }

    private long antallProsessinstanserMedStegOgStatus(ProsessSteg prosessSteg, EnumSet<ProsessStatus> statuser) {
        oppfriskCacheHvisUtløpt();

        long sumAntall = 0;
        for (ProsessStatus prosessStatus : statuser) {
            Pair<ProsessSteg, ProsessStatus> prosessTypeOgSteg = Pair.of(prosessSteg, prosessStatus);
            Long antall = antallPerStegOgStatus.get(prosessTypeOgSteg);
            if (antall != null) {
                sumAntall = sumAntall + antall;
            }
        }
        return sumAntall;
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
        if (alderMs >= maksDataLevetidMs) {
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

        Collection<ProsessinstansStegAntall> prosessinstansMetrikkerForStegOgStatus = prosessinstansRepository.
            antallAktiveOgFeiletPerStegOgStatus(PROSESS_STEG);

        antallPerStegOgStatus = new HashMap<>();
        for (ProsessinstansStegAntall prosessinstansStegAntall : prosessinstansMetrikkerForStegOgStatus) {
            ProsessSteg feiletSteg = ProsessflytDefinisjon.hentNesteSteg(prosessinstansStegAntall.getProsessType(), prosessinstansStegAntall.getSistFullfortSteg())
                .orElse(null);

            if (feiletSteg != null) {
                Pair<ProsessSteg, ProsessStatus> stegOgStatus = Pair.of(feiletSteg, prosessinstansStegAntall.getProsessStatus());
                antallPerStegOgStatus.put(stegOgStatus, prosessinstansStegAntall.getAntall());
            }
        }
    }
}
