package no.nav.melosys.service.aktoer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.DelayQueue;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class AktoerService implements ApplicationListener<ContextRefreshedEvent> {

    private static final long MILLIS_MELLOM_VÅKNE_OPP = 3600000; // 1 time
    private static final long MILLIS_LEVETID_I_CACHE = 28800000; // 8 timer

    private static final Logger log = LoggerFactory.getLogger(AktoerService.class);

    private final TpsFasade tpsFasade;

    private final Map<String, String> identCache = new HashMap<>();
    private final DelayQueue<CacheElement> utløpskø = new DelayQueue<>();

    @Autowired
    public AktoerService(TpsFasade tpsFasade) {
        this.tpsFasade = tpsFasade;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        new TømCacheScheduler().start();
    }

    public String hentAktørIdForIdent(String ident) throws IkkeFunnetException {
        if (identCache.containsKey(ident)) {
            return identCache.get(ident);
        }
        String aktørId = tpsFasade.hentAktørIdForIdent(ident);
        identCache.put(ident, aktørId);
        utløpskø.put(new CacheElement(ident, MILLIS_LEVETID_I_CACHE));
        return aktørId;
    }

    private class TømCacheScheduler extends Thread {
        @Override
        public void run() {
            for (;;) {
                CacheElement element = utløpskø.poll();
                while (element != null) {
                    log.debug("Fjerner ident " + element.hentNøkkel() + " fra cache");
                    identCache.remove(element.hentNøkkel());
                    element = utløpskø.poll();
                }
                try {
                    sleep(MILLIS_MELLOM_VÅKNE_OPP);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
