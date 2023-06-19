package no.nav.melosys.saksflyt.metrikker;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.metrikker.ProsessinstansAntall;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static no.nav.melosys.domain.saksflyt.ProsessStatus.FEILET;
import static no.nav.melosys.domain.saksflyt.ProsessStatus.FERDIG;
import static no.nav.melosys.domain.saksflyt.ProsessType.ANMODNING_OM_UNNTAK;
import static no.nav.melosys.domain.saksflyt.ProsessType.IVERKSETT_VEDTAK_EOS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


@SpringJUnitConfig(classes = ProsessinstansStatusCache.class)
@EnableScheduling
@TestPropertySource(properties = "melosys.prosesser.status.oppfriskning.frekvens=100")
class ProsessinstansStatusCacheTest {
    @MockBean
    private ProsessinstansRepository prosessinstansRepository;

    @Autowired
    ProsessinstansStatusCache cache;

    @Test
    void antallProsessinstanserFeiletPåType() {
        when(prosessinstansRepository.antallAktiveOgFeiletPerTypeOgStatus(any())).thenReturn(getProsessinstansAntallFeilet());
        when(prosessinstansRepository.antallAktiveOgFeiletPerStegOgStatus(anyCollection(), anyBoolean())).thenReturn(Collections.emptyList());


        assertThat(cache.antallProsessinstanserFeiletPåType(ProsessType.ANMODNING_OM_UNNTAK)).isZero();


        await().atMost(Duration.ofSeconds(1))
            .pollDelay(Duration.ofMillis(50))
            .untilAsserted(() -> {
                    assertThat(cache.antallProsessinstanserFeiletPåType(ProsessType.ANMODNING_OM_UNNTAK)).isPositive();
                }
            );
    }

    private static Collection<ProsessinstansAntall> getProsessinstansAntallFeilet() {
        ProsessinstansAntall prosessinstansAntall_1 = new ProsessinstansAntall(ANMODNING_OM_UNNTAK, FERDIG, 0);
        ProsessinstansAntall prosessinstansAntall_2 = new ProsessinstansAntall(ANMODNING_OM_UNNTAK, FEILET, 2);
        ProsessinstansAntall prosessinstansAntall_3 = new ProsessinstansAntall(IVERKSETT_VEDTAK_EOS, FERDIG, 2);
        return Arrays.asList(prosessinstansAntall_1, prosessinstansAntall_2, prosessinstansAntall_3);
    }
}
