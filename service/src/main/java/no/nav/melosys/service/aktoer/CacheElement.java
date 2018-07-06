package no.nav.melosys.service.aktoer;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class CacheElement implements Delayed {

    private final String nøkkel;
    private final long levetid;
    private final long starttid = System.currentTimeMillis();

    public CacheElement(String nøkkel, long levetid) {
        this.nøkkel = nøkkel;
        this.levetid = levetid;
    }

    private long gjenværendeLevetid() {
        return (starttid + levetid) - System.currentTimeMillis();
    }

    @Override
    public long getDelay(TimeUnit timeUnit) {
        return timeUnit.convert(gjenværendeLevetid(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed that) {
        return Long.compare(this.gjenværendeLevetid(), ((CacheElement) that).gjenværendeLevetid());
    }

    public String hentNøkkel() {
        return nøkkel;
    }
}
