package no.nav.melosys.saksflyt;

import java.util.Collections;
import java.util.UUID;

import no.nav.melosys.domain.saksflyt.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProsessinstansBehandlerTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private StegBehandler stegbehandler;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ProsessinstansBehandler prosessinstansBehandler;

    private final Prosessinstans prosessinstans = spy(new Prosessinstans());

    @BeforeEach
    public void setup() {
        when(stegbehandler.inngangsSteg()).thenReturn(ProsessSteg.SED_MOTTAK_RUTING);
        prosessinstansBehandler = new ProsessinstansBehandler(Collections.singleton(stegbehandler), prosessinstansRepository, applicationEventPublisher);

        when(prosessinstans.getId()).thenReturn(UUID.randomUUID());
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setStatus(ProsessStatus.KLAR);
    }

    @Test
    void behandleProsessinstans_nyProsessinstansStegNull_blirBehandlet() {
        when(prosessinstansRepository.save(any(Prosessinstans.class))).thenAnswer(returnsFirstArg());
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        assertThat(prosessinstans.getSistFullførtSteg()).isEqualTo(ProsessSteg.SED_MOTTAK_RUTING);
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.FERDIG);
        verify(stegbehandler).utfør(eq(prosessinstans));
        verify(prosessinstansRepository, times(3)).save(eq(prosessinstans));
    }

    @Test
    void behandleProsessinstans_nyProsessinstansStegNullStegbehandlerKasterFeil_statusFeiletBlirLagretMedHendelse() {
        when(prosessinstansRepository.save(any(Prosessinstans.class))).thenAnswer(returnsFirstArg());
        doThrow(new FunksjonellException("FEIL!")).when(stegbehandler).utfør(eq(prosessinstans));

        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        assertThat(prosessinstans.getSistFullførtSteg()).isNull();
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.FEILET);
        assertThat(prosessinstans.getHendelser()).hasSize(1)
            .flatExtracting(ProsessinstansHendelse::getSteg, ProsessinstansHendelse::getProsessinstans)
            .containsExactly(ProsessSteg.SED_MOTTAK_RUTING, prosessinstans);
        verify(prosessinstansRepository, times(2)).save(eq(prosessinstans));
    }

    @Test
    void behandleProsessinstans_prosessinstansMedStatusFeilet_blirIkkeBehandlet() {
        prosessinstans.setStatus(ProsessStatus.FEILET);
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        verify(stegbehandler, never()).utfør(any());
        verify(prosessinstansRepository, never()).save(any());
    }
}
