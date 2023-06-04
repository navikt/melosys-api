package no.nav.melosys.saksflyt.metrikker;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.domain.metrikker.ProsessinstansAntall;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.saksflyt.ProsessStatus.FEILET;
import static no.nav.melosys.domain.saksflyt.ProsessStatus.FERDIG;
import static no.nav.melosys.domain.saksflyt.ProsessType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProsessinstansStatusCacheTest {
    @Mock
    private ProsessinstansRepository prosessinstansRepository;

    private ProsessinstansStatusCache cache;
    private List<ProsessinstansAntall> prosessinstansMetrikkerList;

    @BeforeEach
    void setup() {
        cache = new ProsessinstansStatusCache(prosessinstansRepository);
        ProsessinstansAntall prosessinstansAntall_1 = new ProsessinstansAntall(JFR_ANDREGANG_REPLIKER_BEHANDLING, FERDIG, 2);
        ProsessinstansAntall prosessinstansAntall_2 = new ProsessinstansAntall(JFR_KNYTT, FEILET, 1);
        ProsessinstansAntall prosessinstansAntall_3 = new ProsessinstansAntall(IVERKSETT_VEDTAK_EOS, FEILET, 2);
        prosessinstansMetrikkerList = Arrays.asList(prosessinstansAntall_1, prosessinstansAntall_2, prosessinstansAntall_3);
    }

    @Test
    void antallProsessinstanserFeilet() {
        when(prosessinstansRepository.antallAktiveOgFeiletPerTypeOgStatus(anyCollection()))
            .thenReturn(prosessinstansMetrikkerList);
        assertThat(cache.antallProsessinstanserFeiletPåType(JFR_ANDREGANG_REPLIKER_BEHANDLING)).isEqualTo(0.0);
        assertThat(cache.antallProsessinstanserFeiletPåType(JFR_KNYTT)).isEqualTo(1.0);
        assertThat(cache.antallProsessinstanserFeiletPåType(IVERKSETT_VEDTAK_EOS)).isEqualTo(2.0);
    }
}
