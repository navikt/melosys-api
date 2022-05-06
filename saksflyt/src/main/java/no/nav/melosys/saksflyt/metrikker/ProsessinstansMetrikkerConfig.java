package no.nav.melosys.saksflyt.metrikker;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.metrics.MetrikkerNavn;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProsessinstansMetrikkerConfig {

    @Bean
    public MeterBinder ProsessinstansMetrikker(MeterRegistry meterRegistry,
                                               ProsessinstansStatusCache statusCache) {
        return registry -> {
            for (ProsessType prosessType : ProsessType.values()) {
                String gaugeNavn = MetrikkerNavn.PROSESSINSTANSER + prosessType.getKode().toLowerCase() + ".feilet";
                Gauge.builder(
                    gaugeNavn,
                    statusCache,
                    type -> statusCache.antallProsessinstanserFeilet(prosessType)
                ).register(meterRegistry);
            }
        };
    }

    @Bean
    public MeterBinder ProsessinstansFeiledeStegMetrikker(MeterRegistry meterRegistry,
                                               ProsessinstansStatusCache statusCache) {
        return registry -> {
            for (ProsessSteg prosessSteg : ProsessSteg.values()) {
                String gaugeNavn = MetrikkerNavn.PROSESSINSTANSER_STEG + prosessSteg.getKode().toLowerCase() + ".feilet";
                Gauge.builder(
                    gaugeNavn,
                    statusCache,
                    type -> statusCache.antallProsessinstanserFeiletPaSteg(prosessSteg)
                ).register(meterRegistry);
            }
        };
    }
}
