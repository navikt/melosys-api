package no.nav.melosys.saksflyt.metrikker;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.saksflyt.ProsessinstansRepository;
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytDefinisjon;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansStatusCache {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansStatusCache.class);

    private final ProsessinstansRepository prosessinstansRepository;
    private final Map<Pair<ProsessType, ProsessStatus>, Long> antallPerTypeOgStatus = new HashMap<>();
    private final Map<Pair<ProsessSteg, ProsessStatus>, Long> antallPerStegOgStatus = new HashMap<>();
    private static final EnumSet<ProsessType> PROSESS_TYPER = EnumSet.allOf(ProsessType.class);
    private static final EnumSet<ProsessSteg> PROSESS_STEG = EnumSet.allOf(ProsessSteg.class);
    private static final EnumSet<ProsessStatus> STATUS_FEILET = EnumSet.of(ProsessStatus.FEILET);

    public ProsessinstansStatusCache(ProsessinstansRepository prosessinstansRepository) {
        this.prosessinstansRepository = prosessinstansRepository;
    }

    double antallProsessinstanserFeiletPåType(ProsessType type) {
        return antallProsessinstanserMedTypeOgStatus(type, STATUS_FEILET);
    }

    double antallProsessinstanserFeiletPåSteg(ProsessSteg prosessSteg) {
        return antallProsessinstanserMedStegOgStatus(prosessSteg, STATUS_FEILET);
    }

    private long antallProsessinstanserMedStegOgStatus(ProsessSteg prosessSteg, EnumSet<ProsessStatus> statuser) {
        long sumAntall = 0;
        for (ProsessStatus prosessStatus : statuser) {
            Pair<ProsessSteg, ProsessStatus> prosessStegOgStatus = Pair.of(prosessSteg, prosessStatus);
            Long antall = antallPerStegOgStatus.get(prosessStegOgStatus);
            if (antall != null) {
                sumAntall = sumAntall + antall;
            }
        }
        return sumAntall;
    }

    private long antallProsessinstanserMedTypeOgStatus(ProsessType prosessType, EnumSet<ProsessStatus> statuser) {
        long sumAntall = 0;
        for (ProsessStatus prosessStatus : statuser) {
            Pair<ProsessType, ProsessStatus> prosessTypeOgStatus = Pair.of(prosessType, prosessStatus);
            Long antall = antallPerTypeOgStatus.get(prosessTypeOgStatus);
            if (antall != null) {
                sumAntall = sumAntall + antall;
            }
        }
        return sumAntall;
    }

    @Scheduled(fixedRateString = "${melosys.prosesser.status.oppfriskning.frekvens:30000}")
    private void oppfriskCache() {
        log.debug("Oppfrisker caching av metrikker for prosessinstanser");
        long tidStart = System.currentTimeMillis();

        oppfriskPerTypeOgStatus();

        oppfriskPerStegOgStatus();

        long tidSlutt = System.currentTimeMillis();
        long tidBrukt = tidSlutt - tidStart;
        log.debug("Oppfriskning av cache av metrikker for prosessinstanser tok {} millisekunder.", tidBrukt);
    }

    private void oppfriskPerTypeOgStatus() {
        Collection<ProsessinstansAntall> prosessinstansMetrikker = prosessinstansRepository.
            antallAktiveOgFeiletPerTypeOgStatus(PROSESS_TYPER);

        antallPerTypeOgStatus.clear();
        for (ProsessinstansAntall prosessinstansAntall : prosessinstansMetrikker) {
            Pair<ProsessType, ProsessStatus> typeOgStatus = Pair.of(prosessinstansAntall.getProsessType(), prosessinstansAntall.getProsessStatus());
            antallPerTypeOgStatus.put(typeOgStatus, prosessinstansAntall.getAntall());
        }
    }

    private void oppfriskPerStegOgStatus() {
        Collection<ProsessinstansStegAntall> prosessinstansMetrikkerForStegOgStatus = prosessinstansRepository.
            antallAktiveOgFeiletPerStegOgStatus(PROSESS_STEG, true);

        antallPerStegOgStatus.clear();
        for (ProsessinstansStegAntall prosessinstansStegAntall : prosessinstansMetrikkerForStegOgStatus) {
            ProsessSteg feiletSteg = ProsessflytDefinisjon.hentNesteSteg(prosessinstansStegAntall.getProsessType(), prosessinstansStegAntall.getSistFullfortSteg())
                .orElse(null);

            if (feiletSteg != null) {
                Pair<ProsessSteg, ProsessStatus> stegOgStatus = Pair.of(feiletSteg, prosessinstansStegAntall.getProsessStatus());
                antallPerStegOgStatus.compute(stegOgStatus,
                    (eksisterendeStegOgStatus, eksisterendeAntall) -> eksisterendeAntall == null
                        ? prosessinstansStegAntall.getAntall()
                        : eksisterendeAntall + prosessinstansStegAntall.getAntall());
            }
        }
    }
}
