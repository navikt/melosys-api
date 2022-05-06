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
                String gaugeNavnProsessinstansType = MetrikkerNavn.PROSESSINSTANSER + prosessType.getKode().toLowerCase() + ".feilet";
                Gauge.builder(
                    gaugeNavnProsessinstansType,
                    statusCache,
                    type -> statusCache.antallProsessinstanserFeilet(prosessType)
                ).register(meterRegistry);

                for (ProsessSteg prosessSteg : ProsessSteg.values()) {
                    String gaugeNavnProsessinstansSteg = MetrikkerNavn.PROSESSINSTANSER_STEG + prosessSteg.getKode().toLowerCase() + ".feilet";
                    Gauge.builder(
                        gaugeNavnProsessinstansSteg,
                        statusCache,
                        type -> statusCache.antallProsessinstanserFeiletPaSteg(prosessSteg)
                    ).register(meterRegistry);
                }
            }
        };
    }
}
