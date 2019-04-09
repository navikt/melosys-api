package no.nav.melosys.integrasjon.tps.aktoer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AktoerIdCache {

    private static final Logger log = LoggerFactory.getLogger(AktoerIdCache.class);

    private final Map<String, String> identTilAktørIdCache;
    private final Map<String, String> aktørIdTilIdentCache;
    private final DelayQueue<AktoerIdCacheElement> utløpskø = new DelayQueue<>();

    private final long millisMellomVåkneOpp;
    private final long millisLevetidICache;

    public AktoerIdCache(@Value("${melosys.service.aktørid.millisMellomVåkneOpp}") long millisMellomVåkneOpp,
                         @Value("${melosys.service.aktørid.millisLevetidICache}") long millisLevetidICache,
                         @Value("${melosys.service.aktørid.initiellKapasitetICache}") int initiellKapasitet) {
        this.millisMellomVåkneOpp = millisMellomVåkneOpp;
        this.millisLevetidICache = millisLevetidICache;

        identTilAktørIdCache = new ConcurrentHashMap<>(initiellKapasitet);
        aktørIdTilIdentCache = new ConcurrentHashMap<>(initiellKapasitet);
    }

    public void leggTilCache(String ident, String aktørId) {
        if (ident == null) {
            log.warn("Forsøk å cache null for aktørId {}!", aktørId);
            return;
        }
        if (aktørId == null) {
            log.warn("Forsøk å cache null for ident {}!", ident);
            return;
        }
        identTilAktørIdCache.put(ident, aktørId);
        aktørIdTilIdentCache.put(aktørId, ident);
        utløpskø.put(new AktoerIdCacheElement(ident, millisLevetidICache));
    }

    public String hentAktørIdFraCache(String ident) {
        return identTilAktørIdCache.get(ident);
    }

    public String hentIdentFraCache(String aktørId) {
        return aktørIdTilIdentCache.get(aktørId);
    }

    @EventListener
    @SuppressWarnings("unused")
    public void onApplicationEvent(ApplicationReadyEvent event) {
        new TømCacheScheduler().start();
    }

    private class TømCacheScheduler extends Thread {
        @Override
        public void run() {
            for (; ; ) {
                AktoerIdCacheElement element = utløpskø.poll();
                while (element != null) {
                    if (isInterrupted()) {
                        return;
                    }
                    String ident = element.hentNøkkel();
                    String aktørId = identTilAktørIdCache.get(ident);
                    log.debug("Fjerner ident {} og aktørId {} fra cache", ident, aktørId);
                    identTilAktørIdCache.remove(ident);
                    aktørIdTilIdentCache.remove(aktørId);
                    element = utløpskø.poll();
                }
                try {
                    sleep(millisMellomVåkneOpp);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
