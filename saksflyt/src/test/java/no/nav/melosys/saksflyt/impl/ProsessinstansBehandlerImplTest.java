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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ProsessinstansBehandlerImplTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private StegBehandler mangelBrevStebehandler;

    private ProsessinstansBehandler prosessinstansBehandler;

    private final Prosessinstans prosessinstans = spy(new Prosessinstans());

    @Before
    public void setup() {
        when(mangelBrevStebehandler.inngangsSteg()).thenReturn(ProsessSteg.MANGELBREV);
        when(prosessinstansRepository.save(any(Prosessinstans.class))).thenAnswer(returnsFirstArg());
        prosessinstansBehandler = new ProsessinstansBehandlerImpl(Collections.singleton(mangelBrevStebehandler), prosessinstansRepository);

        when(prosessinstans.getId()).thenReturn(UUID.randomUUID());
        prosessinstans.setType(ProsessType.MANGELBREV);
        prosessinstans.setStatus(ProsessStatus.KLAR);
    }

    @Test
    public void behandleProsessinstans_nyProsessinstansStegNull_blirBehandlet() throws MelosysException {
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        assertThat(prosessinstans.getSistFullførtSteg()).isEqualTo(ProsessSteg.MANGELBREV);
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.FERDIG);
        verify(mangelBrevStebehandler).utfør(eq(prosessinstans));
        verify(prosessinstansRepository, times(2)).save(eq(prosessinstans));
    }

    @Test
    public void behandleProsessinstans_nyProsessinstansStegNullStegbehandlerKasterFeil_statusFeiletBlirLagretMedHendelse() throws MelosysException {
        doThrow(new FunksjonellException("FEIL!")).when(mangelBrevStebehandler).utfør(eq(prosessinstans));

        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        assertThat(prosessinstans.getSistFullførtSteg()).isNull();
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.FEILET);
        assertThat(prosessinstans.getHendelser()).hasSize(1)
            .flatExtracting(ProsessinstansHendelse::getSteg, ProsessinstansHendelse::getProsessinstans)
            .containsExactly(ProsessSteg.MANGELBREV, prosessinstans);
        verify(prosessinstansRepository).save(eq(prosessinstans));
    }

    @Test
    public void behandleProsessinstans_prosessinstansMedStatusFeilet_blirIkkeBehandlet() throws MelosysException {
        prosessinstans.setStatus(ProsessStatus.FEILET);
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        verify(mangelBrevStebehandler, never()).utfør(any());
        verify(prosessinstansRepository, never()).save(any());
    }
}