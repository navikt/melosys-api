package no.nav.melosys.saksflyt.impl;

import java.util.Set;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.saksflyt.ProsessinstansInfo;
import no.nav.melosys.domain.saksflyt.ProsessinstansLåsType;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import no.nav.melosys.service.saksflyt.ProsessinstansOpprettetEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProsessinstansOpprettetListenerTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private ProsessinstansBehandler prosessinstansBehandler;

    private ProsessinstansOpprettetListener prosessinstansOpprettetListener;

    private Prosessinstans prosessinstans;

    @BeforeEach
    void setup() {
        prosessinstansOpprettetListener = new ProsessinstansOpprettetListener(prosessinstansBehandler, prosessinstansRepository);
        prosessinstans = new Prosessinstans();
        prosessinstans.setId(UUID.randomUUID());
    }

    @Test
    void oppdaterProsessinstansstatus_harIkkeLås_settesIkkePåVent() {
        prosessinstans.setStatus(ProsessStatus.KLAR);
        prosessinstansOpprettetListener.oppdaterProsessinstansstatus(new ProsessinstansOpprettetEvent(prosessinstans));
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.KLAR);
        verifyNoInteractions(prosessinstansRepository, prosessinstansBehandler);
    }

    @Test
    void oppdaterProsessinstansstatus_finnesProsessMedSammeReferanseUnderBehandling_settesIkkePåVent() {
        prosessinstans.setStatus(ProsessStatus.KLAR);
        final var låsReferanse = "12_12_1";
        prosessinstans.setLåsType(ProsessinstansLåsType.SED);
        prosessinstans.setLåsReferanse(låsReferanse);

        var eksisterendeProsessinstans = prosessinstans(låsReferanse, ProsessStatus.UNDER_BEHANDLING);
        when(prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(any(), any())).thenReturn(Set.of(new ProsessinstansInfo(eksisterendeProsessinstans)));

        prosessinstansOpprettetListener.oppdaterProsessinstansstatus(new ProsessinstansOpprettetEvent(prosessinstans));
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.KLAR);
    }

    @Test
    void oppdaterProsessinstansstatus_finnesProsessMedSammeReferanseUlikId_settesPåVent() {
        prosessinstans.setStatus(ProsessStatus.KLAR);
        final var låsReferanse = "12_12_1";
        prosessinstans.setLåsType(ProsessinstansLåsType.SED);
        prosessinstans.setLåsReferanse(låsReferanse);

        var eksisterendeProsessinstans = prosessinstans("12_13_1", ProsessStatus.UNDER_BEHANDLING);
        when(prosessinstansRepository.findAllByStatusNotInAndLåsReferanseStartingWith(any(), any())).thenReturn(Set.of(new ProsessinstansInfo(eksisterendeProsessinstans)));

        prosessinstansOpprettetListener.oppdaterProsessinstansstatus(new ProsessinstansOpprettetEvent(prosessinstans));
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.PÅ_VENT);
    }

    @Test
    void behandleOpprettetProsessinstans_statusErKlar_behandlesVidere() {
        prosessinstans.setStatus(ProsessStatus.KLAR);
        prosessinstansOpprettetListener.behandleOpprettetProsessinstans(new ProsessinstansOpprettetEvent(prosessinstans));
        verify(prosessinstansBehandler).behandleProsessinstans(prosessinstans);
    }

    @Test
    void behandleOpprettetProsessinstans_statusErPåVent_behandlesIkke() {
        prosessinstans.setStatus(ProsessStatus.PÅ_VENT);
        prosessinstansOpprettetListener.behandleOpprettetProsessinstans(new ProsessinstansOpprettetEvent(prosessinstans));
        verifyNoInteractions(prosessinstansBehandler);
    }

    private Prosessinstans prosessinstans(String låsReferanse, ProsessStatus prosessStatus) {
        var prosessinstans = new Prosessinstans();
        prosessinstans.setId(UUID.randomUUID());
        prosessinstans.setLåsType(ProsessinstansLåsType.SED);
        prosessinstans.setLåsReferanse(låsReferanse);
        prosessinstans.setStatus(prosessStatus);
        return prosessinstans;
    }
}