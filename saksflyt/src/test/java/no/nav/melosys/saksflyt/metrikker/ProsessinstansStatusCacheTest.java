package no.nav.melosys.saksflyt.metrikker;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.repository.ProsessinstansAntall;
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
public class ProsessinstansStatusCacheTest {
    @Mock
    private ProsessinstansRepository prosessinstansRepository;

    private ProsessinstansStatusCache cache;
    private List<ProsessinstansAntall> prosessinstansMetrikkerList;

    @BeforeEach
    public void setup() {
        cache = new ProsessinstansStatusCache(prosessinstansRepository, 100);
        ProsessinstansAntall prosessinstansAntall_1 = new ProsessinstansAntall(JFR_NY_VURDERING, FERDIG, 2);
        ProsessinstansAntall prosessinstansAntall_2 = new ProsessinstansAntall(JFR_KNYTT, FEILET, 1);
        ProsessinstansAntall prosessinstansAntall_3 = new ProsessinstansAntall(IVERKSETT_VEDTAK_EOS, FEILET, 2);
        prosessinstansMetrikkerList = Arrays.asList(prosessinstansAntall_1, prosessinstansAntall_2, prosessinstansAntall_3);
    }

    @Test
    public void antallProsessinstanserFeilet() {
        when(prosessinstansRepository.antallAktiveOgFeiletPerTypeOgStatus(anyCollection()))
            .thenReturn(prosessinstansMetrikkerList);
        assertThat(cache.antallProsessinstanserFeilet(JFR_NY_VURDERING)).isEqualTo(0.0);
        assertThat(cache.antallProsessinstanserFeilet(JFR_KNYTT)).isEqualTo(1.0);
        assertThat(cache.antallProsessinstanserFeilet(IVERKSETT_VEDTAK_EOS)).isEqualTo(2.0);
    }
}
