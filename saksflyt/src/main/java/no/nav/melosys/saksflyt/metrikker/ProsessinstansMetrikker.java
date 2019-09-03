package no.nav.melosys.saksflyt.metrikker;

import javax.annotation.PostConstruct;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.metrics.MetrikkerNavn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProsessinstansMetrikker {
    private final MeterRegistry meterRegistry;
    private final ProsessinstansStatusCache statusCache;

    @Autowired
    public ProsessinstansMetrikker(MeterRegistry meterRegistry, ProsessinstansStatusCache prosessinstansStatusCache) {
        this.meterRegistry = meterRegistry;
        this.statusCache = prosessinstansStatusCache;
    }

    @PostConstruct
    public void init() {
        for (ProsessType prosessType : ProsessType.values()) {
            String gaugeNavn = MetrikkerNavn.PROSESSINSTANSER + prosessType.getKode().toLowerCase() + ".feilet";
            Gauge.builder(gaugeNavn, this, m -> statusCache.antallProsessinstanserFeilet(prosessType)).register(meterRegistry);
        }
    }
}
