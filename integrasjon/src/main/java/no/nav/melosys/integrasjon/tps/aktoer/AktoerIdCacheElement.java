package no.nav.melosys.integrasjon.tps.aktoer;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class AktoerIdCacheElement implements Delayed {

    private final String nøkkel;
    private final long utløpstidspunkt;

    public AktoerIdCacheElement(String nøkkel, long levetid) {
        this.nøkkel = nøkkel;
        this.utløpstidspunkt = System.currentTimeMillis() + levetid;
    }

    private long gjenværendeLevetid() {
        return utløpstidspunkt - System.currentTimeMillis();
    }

    @Override
    public long getDelay(TimeUnit timeUnit) {
        return timeUnit.convert(gjenværendeLevetid(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed that) {
        return Long.compare(this.gjenværendeLevetid(), ((AktoerIdCacheElement) that).gjenværendeLevetid());
    }

    public String hentNøkkel() {
        return nøkkel;
    }
}
