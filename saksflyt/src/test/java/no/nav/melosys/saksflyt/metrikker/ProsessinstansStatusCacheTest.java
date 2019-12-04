package no.nav.melosys.saksflyt.metrikker;

import java.util.Arrays;
import java.util.List;

import no.nav.melosys.repository.ProsessinstansAntall;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;
import static no.nav.melosys.domain.saksflyt.ProsessType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProsessinstansStatusCacheTest {
    @Mock
    private ProsessinstansRepository prosessinstansRepository;

    private ProsessinstansStatusCache cache;
    private List<ProsessinstansAntall> prosessinstansMetrikkerList;

    @Before
    public void setup() {
        cache = new ProsessinstansStatusCache(prosessinstansRepository, 100);
        ProsessinstansAntall prosessinstansAntall_1 = new ProsessinstansAntall(JFR_NY_BEHANDLING, JFR_OPPRETT_GSAK_SAK, 2);
        ProsessinstansAntall prosessinstansAntall_2 = new ProsessinstansAntall(JFR_KNYTT, JFR_FERDIGSTILL_JOURNALPOST, 1);
        ProsessinstansAntall prosessinstansAntall_3 = new ProsessinstansAntall(IVERKSETT_VEDTAK, FEILET_MASKINELT, 2);
        prosessinstansMetrikkerList = Arrays.asList(prosessinstansAntall_1, prosessinstansAntall_2, prosessinstansAntall_3);
    }

    @Test
    public void antallProsessinstanserFeilet() {
        when(prosessinstansRepository.antallAktiveOgFeiletPerTypeOgSteg()).thenReturn(prosessinstansMetrikkerList);
        assertThat(cache.antallProsessinstanserFeilet(IVERKSETT_VEDTAK)).isEqualTo(2.0);
    }
}