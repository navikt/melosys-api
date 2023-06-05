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
            registrerAntallFeiledeProsessinstanserGruppertPåType(meterRegistry, statusCache);
            registrerAntallFeiledeProsessinstanserGruppertPåSteg(meterRegistry, statusCache);
        };
    }

    private void registrerAntallFeiledeProsessinstanserGruppertPåType(MeterRegistry meterRegistry,
                                                                      ProsessinstansStatusCache statusCache) {
        for (ProsessType prosessType : ProsessType.values()) {
            String gaugeNavnProsessinstansType = MetrikkerNavn.PROSESSINSTANSER_FEILET;
            Gauge.builder(
                gaugeNavnProsessinstansType,
                statusCache,
                type -> statusCache.antallProsessinstanserFeiletPåType(prosessType)
            )
                .tag(MetrikkerNavn.TAG_PROSESSINSTANSTYPE, prosessType.getKode())
                .register(meterRegistry);
        }
    }

    private void registrerAntallFeiledeProsessinstanserGruppertPåSteg(MeterRegistry meterRegistry,
                                                                      ProsessinstansStatusCache statusCache) {
        for (ProsessSteg prosessSteg : ProsessSteg.values()) {
            String gaugeNavnProsessinstansSteg = MetrikkerNavn.PROSESSINSTANSER_STEG + prosessSteg.getKode().toLowerCase() + ".feilet";
            Gauge.builder(
                gaugeNavnProsessinstansSteg,
                statusCache,
                type -> statusCache.antallProsessinstanserFeiletPåSteg(prosessSteg)
            ).register(meterRegistry);
        }
    }
}
