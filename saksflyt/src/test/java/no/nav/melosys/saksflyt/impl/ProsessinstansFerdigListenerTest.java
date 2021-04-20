package no.nav.melosys.saksflyt.impl;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import no.nav.melosys.service.saksflyt.ProsessinstansFerdigEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProsessinstansFerdigListenerTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private ProsessinstansBehandler prosessinstansBehandler;

    private ProsessinstansFerdigListener prosessinstansFerdigListener;

    private Prosessinstans ferdigProsessinstans;

    @BeforeEach
    void setup() {
        prosessinstansFerdigListener = new ProsessinstansFerdigListener(prosessinstansRepository, prosessinstansBehandler);
        ferdigProsessinstans = new Prosessinstans();
        ferdigProsessinstans.setId(UUID.randomUUID());
    }

    @Test
    void prosessinstansFerdig_harIngenLås_gjørIngenting() {
        prosessinstansFerdigListener.prosessinstansFerdig(new ProsessinstansFerdigEvent(ferdigProsessinstans));
        verifyNoInteractions(prosessinstansRepository, prosessinstansBehandler);
    }

    @Test
    void prosesssinstansFerdig_harLåsFinnesAktiveReferanser_gjørIngenting() {
        ferdigProsessinstans.setLåsReferanse("12_12_1");
        when(prosessinstansRepository.existsByStatusNotInAndLåsReferanse(any(), any())).thenReturn(true);

        prosessinstansFerdigListener.prosessinstansFerdig(new ProsessinstansFerdigEvent(ferdigProsessinstans));
        verify(prosessinstansRepository).existsByStatusNotInAndLåsReferanse(any(), any());
        verifyNoMoreInteractions(prosessinstansRepository, prosessinstansBehandler);
    }

    @Test
    void prosessinstansFerdig_harLåsIngenAktiveReferanser_starterTidligstOpprettetProsessinstans() {
        ferdigProsessinstans.setLåsReferanse("12_12_1");

        var prosessinstansUlikReferanse = prosessinstans(LocalDateTime.now().minusDays(2), "13_12_1");
        var tidligstOpprettetProsessinstans = prosessinstans(LocalDateTime.now().minusDays(1), "12_13_1");
        var senestOpprettetProsessinstans = prosessinstans(LocalDateTime.now(), "12_14_1");

        when(prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT)).thenReturn(
            Set.of(prosessinstansUlikReferanse, tidligstOpprettetProsessinstans, senestOpprettetProsessinstans)
        );

        prosessinstansFerdigListener.prosessinstansFerdig(new ProsessinstansFerdigEvent(ferdigProsessinstans));
        verify(prosessinstansBehandler).behandleProsessinstans(tidligstOpprettetProsessinstans);
        assertThat(tidligstOpprettetProsessinstans.getStatus()).isEqualTo(ProsessStatus.KLAR);
    }

    private Prosessinstans prosessinstans(LocalDateTime registrertDato, String referanse) {
        var prosessinstans = new Prosessinstans();
        prosessinstans.setStatus(ProsessStatus.PÅ_VENT);
        prosessinstans.setLåsReferanse(referanse);
        prosessinstans.setRegistrertDato(registrertDato);
        return prosessinstans;
    }
}