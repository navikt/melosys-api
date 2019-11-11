package no.nav.melosys.integrasjonstest.felles.verifisering;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.integrasjonstest.felles.utils.SaksflytTestUtils.hentProsessStegForBehandling;

public class ResultatPoller {
    private static final Logger logger = LoggerFactory.getLogger(ResultatPoller.class);

    private final int sjekkintervallMillisekunder = 3000;
    private final long timeoutMillisekunder;
    private long tidspunktStartet;

    private ResultatPoller(int sekunderTilTimout) {
        this.tidspunktStartet = System.currentTimeMillis();
        this.timeoutMillisekunder = ((long) sekunderTilTimout)*1000;
    }

    public static ResultatPoller Resultatpoller() {
        return new ResultatPoller(120);
    }

    public static ResultatPoller Resultatpoller(int sekunderTilTimout) {
        return new ResultatPoller(sekunderTilTimout);
    }

    public void følg(ProsessinstansRepository repository, long behandlingId, ProsessSteg... prosessSteg) throws InterruptedException {
        long påløptTid = 0;
        List<ProsessSteg> forventedeProsessSteg = Arrays.asList(ProsessSteg.FERDIG, ProsessSteg.FEILET_MASKINELT);
        forventedeProsessSteg.addAll(Arrays.asList(prosessSteg));

        while(påløptTid < timeoutMillisekunder) {
            List<ProsessSteg> lagredeProsessSteg = hentProsessStegForBehandling(repository, behandlingId);
            if (harMinstEnFelles(lagredeProsessSteg, forventedeProsessSteg)) {
                return;
            }

            logger.info("Ventet {} sekunder på prosessinstans for behandlingsid {}", påløptTid/1000, behandlingId);
            Thread.sleep(sjekkintervallMillisekunder);
            påløptTid = System.currentTimeMillis() - tidspunktStartet;
        }
    }

    private static boolean harMinstEnFelles(List<ProsessSteg> lagredeProsessSteg, List<ProsessSteg> forventedeProsessSteg) {
        return !ListUtils.intersection(lagredeProsessSteg, forventedeProsessSteg).isEmpty();
    }
}