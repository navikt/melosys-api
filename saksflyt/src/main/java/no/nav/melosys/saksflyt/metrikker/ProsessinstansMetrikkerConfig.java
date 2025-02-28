package no.nav.melosys.saksflyt.metrikker;

import java.util.List;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.annotation.PostConstruct;
import no.nav.melosys.metrics.MetrikkerNavn;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProsessinstansMetrikkerConfig {

    @Bean
    public MeterBinder ProsessinstansMetrikker(ProsessinstansStatusCache statusCache) {
        return registry -> {
            registrerAntallFeiledeProsessinstanserGruppertPåType(registry, statusCache);
            registrerAntallFeiledeProsessinstanserGruppertPåSteg(registry, statusCache);
        };
    }

    @PostConstruct
    public void init() {
        for (ProsessType prosessType : ProsessType.values()) {
            Metrics.counter(MetrikkerNavn.PROSESSINSTANSER_OPPRETTET, MetrikkerNavn.TAG_TYPE, prosessType.name());
        }
        for (ProsessSteg prosessSteg : ProsessSteg.values()) {
            for (String status : List.of("ok", "feil")) {
                Metrics.counter(
                    MetrikkerNavn.PROSESSINSTANSER_STEG_UTFØRT,
                    MetrikkerNavn.TAG_TYPE, prosessSteg.name(),
                    MetrikkerNavn.TAG_STATUS, status
                );
            }
        }
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
            String gaugeNavnProsessinstansSteg = MetrikkerNavn.PROSESSINSTANSER_STEG_FEILET;
            Gauge.builder(
                    gaugeNavnProsessinstansSteg,
                    statusCache,
                    type -> statusCache.antallProsessinstanserFeiletPåSteg(prosessSteg)
                ).tag(MetrikkerNavn.TAG_PROSESSTEG, prosessSteg.getKode())
                .register(meterRegistry);
        }
    }
}
