package no.nav.melosys.saksflyt.impl;

import java.util.Collections;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.ProsessinstansBehandler;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProsessinstansBehandlerImplTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private StegBehandler SED_MOTTAK_RUTINGStebehandler;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ProsessinstansBehandler prosessinstansBehandler;

    private final Prosessinstans prosessinstans = spy(new Prosessinstans());

    @Before
    public void setup() {
        when(SED_MOTTAK_RUTINGStebehandler.inngangsSteg()).thenReturn(ProsessSteg.SED_MOTTAK_RUTING);
        when(prosessinstansRepository.save(any(Prosessinstans.class))).thenAnswer(returnsFirstArg());
        prosessinstansBehandler = new ProsessinstansBehandlerImpl(Collections.singleton(SED_MOTTAK_RUTINGStebehandler), prosessinstansRepository, applicationEventPublisher);

        when(prosessinstans.getId()).thenReturn(UUID.randomUUID());
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setStatus(ProsessStatus.KLAR);
    }

    @Test
    public void behandleProsessinstans_nyProsessinstansStegNull_blirBehandlet() throws MelosysException {
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        assertThat(prosessinstans.getSistFullførtSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_RUTING);
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.FERDIG);
        verify(SED_MOTTAK_RUTINGStebehandler).utfør(eq(prosessinstans));
        verify(prosessinstansRepository, times(2)).save(eq(prosessinstans));
    }

    @Test
    public void behandleProsessinstans_nyProsessinstansStegNullStegbehandlerKasterFeil_statusFeiletBlirLagretMedHendelse() throws MelosysException {
        doThrow(new FunksjonellException("FEIL!")).when(SED_MOTTAK_RUTINGStebehandler).utfør(eq(prosessinstans));

        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        assertThat(prosessinstans.getSistFullførtSteg()).isNull();
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.FEILET);
        assertThat(prosessinstans.getHendelser()).hasSize(1)
            .flatExtracting(ProsessinstansHendelse::getSteg, ProsessinstansHendelse::getProsessinstans)
            .containsExactly(ProsessSteg.SED_MOTTAK_RUTING, prosessinstans);
        verify(prosessinstansRepository).save(eq(prosessinstans));
    }

    @Test
    public void behandleProsessinstans_prosessinstansMedStatusFeilet_blirIkkeBehandlet() throws MelosysException {
        prosessinstans.setStatus(ProsessStatus.FEILET);
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        verify(SED_MOTTAK_RUTINGStebehandler, never()).utfør(any());
        verify(prosessinstansRepository, never()).save(any());
    }
}