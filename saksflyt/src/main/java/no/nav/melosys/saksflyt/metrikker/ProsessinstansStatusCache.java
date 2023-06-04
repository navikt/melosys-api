package no.nav.melosys.saksflyt.metrikker;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import no.nav.melosys.domain.metrikker.ProsessinstansAntall;
import no.nav.melosys.domain.metrikker.ProsessinstansStegAntall;
import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.prosessflyt.ProsessflytDefinisjon;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansStatusCache {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansStatusCache.class);

    private final ProsessinstansRepository prosessinstansRepository;
    private Map<Pair<ProsessType, ProsessStatus>, Long> antallPerTypeOgStatus;
    private Map<Pair<ProsessSteg, ProsessStatus>, Long> antallPerStegOgStatus;
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

    @Scheduled(fixedRate = 15000)
    private void oppfriskCache() {
        log.info("Oppfrisker caching av metrikker for prosessinstanser");
        long tidStart = System.currentTimeMillis();

        Collection<ProsessinstansAntall> prosessinstansMetrikker = prosessinstansRepository.
            antallAktiveOgFeiletPerTypeOgStatus(PROSESS_TYPER);

        antallPerTypeOgStatus = new HashMap<>();
        for (ProsessinstansAntall prosessinstansAntall : prosessinstansMetrikker) {
            Pair<ProsessType, ProsessStatus> typeOgStatus = Pair.of(prosessinstansAntall.getProsessType(), prosessinstansAntall.getProsessStatus());
            antallPerTypeOgStatus.put(typeOgStatus, prosessinstansAntall.getAntall());
        }

        Collection<ProsessinstansStegAntall> prosessinstansMetrikkerForStegOgStatus = prosessinstansRepository.
            antallAktiveOgFeiletPerStegOgStatus(PROSESS_STEG, true);

        antallPerStegOgStatus = new HashMap<>();
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
        long tidSlutt = System.currentTimeMillis();
        long tidBrukt = tidSlutt - tidStart;
        log.info("Oppfriskning av cache av metrikker for prosessinstanser tok " + tidBrukt + " millisekunder.");
    }
}
