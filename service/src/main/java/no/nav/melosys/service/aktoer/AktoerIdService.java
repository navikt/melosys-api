package no.nav.melosys.service.aktoer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Service
public class AktoerIdService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AktoerIdService.class);

    private final TpsFasade tpsFasade;

    private final Map<String, String> identCache = new ConcurrentHashMap<>();
    private final DelayQueue<CacheElement> utløpskø = new DelayQueue<>();

    @Value("${melosys.service.aktørid.millisMellomVåkneOpp}")
    private long millisMellomVåkneOpp;

    @Value("${melosys.service.millisLevetidICache}")
    private long millisLevetidICache;

    @Autowired
    public AktoerIdService(TpsFasade tpsFasade) {
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
        utløpskø.put(new CacheElement(ident, millisLevetidICache));
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
                    sleep(millisMellomVåkneOpp);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
