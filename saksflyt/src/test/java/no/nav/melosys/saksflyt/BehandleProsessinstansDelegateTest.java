package no.nav.melosys.saksflyt;

import java.util.Set;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.saksflyt.ProsessinstansInfo;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandleProsessinstansDelegateTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private ProsessinstansBehandler prosessinstansBehandler;

    private BehandleProsessinstansDelegate behandleProsessinstansDelegate;

    private Prosessinstans prosessinstans;

    @BeforeEach
    void setup() {
        behandleProsessinstansDelegate = new BehandleProsessinstansDelegate(prosessinstansBehandler, prosessinstansRepository);
        prosessinstans = new Prosessinstans();
        prosessinstans.setId(UUID.randomUUID());
    }

    @Test
    void oppdaterStatusOmSkalPåVent_harIkkeLås_settesIkkePåVent() {
        prosessinstans.setStatus(ProsessStatus.KLAR);
        behandleProsessinstansDelegate.oppdaterStatusOmSkalPåVent(prosessinstans);
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.KLAR);
        verifyNoInteractions(prosessinstansRepository, prosessinstansBehandler);
    }

    @Test
    void oppdaterStatusOmSkalPåVent_finnesProsessMedSammeReferanseUnderBehandling_settesIkkePåVent() {
        prosessinstans.setStatus(ProsessStatus.KLAR);
        final var låsReferanse = "12_12_1";
        prosessinstans.setLåsReferanse(låsReferanse);

        var eksisterendeProsessinstans = prosessinstans(låsReferanse);
        when(prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(eq(prosessinstans.getId()), any(), any()))
            .thenReturn(Set.of(new ProsessinstansInfo(eksisterendeProsessinstans)));

        behandleProsessinstansDelegate.oppdaterStatusOmSkalPåVent(prosessinstans);
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.KLAR);
    }

    @Test
    void oppdaterStatusOmSkalPåVent_finnesProsessMedSammeReferanseUlikId_settesPåVent() {
        prosessinstans.setStatus(ProsessStatus.KLAR);
        final var låsReferanse = "12_12_1";
        prosessinstans.setLåsReferanse(låsReferanse);

        var eksisterendeProsessinstans = prosessinstans("12_13_1");
        when(prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(eq(prosessinstans.getId()), any(), any()))
            .thenReturn(Set.of(new ProsessinstansInfo(eksisterendeProsessinstans)));

        behandleProsessinstansDelegate.oppdaterStatusOmSkalPåVent(prosessinstans);
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.PÅ_VENT);
        verify(prosessinstansRepository).save(prosessinstans);
    }

    @Test
    void behandleOpprettetProsessinstans_statusErKlar_behandlesVidere() {
        prosessinstans.setStatus(ProsessStatus.KLAR);
        behandleProsessinstansDelegate.behandleProsessinstansHvisKlar(prosessinstans);
        verify(prosessinstansBehandler).behandleProsessinstans(prosessinstans);
    }

    @Test
    void behandleOpprettetProsessinstans_statusErPåVent_behandlesIkke() {
        prosessinstans.setStatus(ProsessStatus.PÅ_VENT);
        behandleProsessinstansDelegate.behandleProsessinstansHvisKlar(prosessinstans);
        verifyNoInteractions(prosessinstansBehandler);
    }

    private Prosessinstans prosessinstans(String låsReferanse) {
        var prosessinstans = new Prosessinstans();
        prosessinstans.setId(UUID.randomUUID());
        prosessinstans.setLåsReferanse(låsReferanse);
        prosessinstans.setStatus(ProsessStatus.UNDER_BEHANDLING);
        return prosessinstans;
    }
}
