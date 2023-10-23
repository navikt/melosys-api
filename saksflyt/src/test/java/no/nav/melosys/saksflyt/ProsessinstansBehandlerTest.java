package no.nav.melosys.saksflyt;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
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
        verify(stegbehandler).utfør(prosessinstans);
        verify(prosessinstansRepository, times(3)).save(prosessinstans);
    }

    @Test
    void behandleProsessinstans_nyProsessinstansStegNullStegbehandlerKasterFeil_statusFeiletBlirLagretMedHendelse() {
        when(prosessinstansRepository.save(any(Prosessinstans.class))).thenAnswer(returnsFirstArg());
        doThrow(new FunksjonellException("FEIL!")).when(stegbehandler).utfør(prosessinstans);

        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        assertThat(prosessinstans.getSistFullførtSteg()).isNull();
        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.FEILET);
        assertThat(prosessinstans.getHendelser()).hasSize(1)
            .flatExtracting(ProsessinstansHendelse::getSteg, ProsessinstansHendelse::getProsessinstans)
            .containsExactly(ProsessSteg.SED_MOTTAK_RUTING, prosessinstans);
        verify(prosessinstansRepository, times(2)).save(prosessinstans);
    }

    @Test
    void behandleProsessinstans_prosessinstansMedStatusFeilet_blirIkkeBehandlet() {
        prosessinstans.setStatus(ProsessStatus.FEILET);
        prosessinstansBehandler.behandleProsessinstans(prosessinstans);
        verify(stegbehandler, never()).utfør(any());
        verify(prosessinstansRepository, never()).save(any());
    }

    @Test
    void gjenopprettProsesserSomHengerVedOppstart() {
        Prosessinstans prosessinstans1 = lagProsessinstans(LocalDateTime.now().minusHours(12));
        Prosessinstans prosessinstans2 = lagProsessinstans(LocalDateTime.MIN);
        when(prosessinstansRepository.findAllByStatusIn(anySet())).thenReturn(Set.of(prosessinstans1, prosessinstans2));

        prosessinstansBehandler.gjenopprettProsesserSomHengerVedOppstart(null);

        verify(prosessinstansRepository).save(prosessinstans2);
        verify(applicationEventPublisher).publishEvent(any());
    }

    private Prosessinstans lagProsessinstans(LocalDateTime endretDato) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setId(UUID.randomUUID());
        prosessinstans.setBehandling(null);
        prosessinstans.setStatus(ProsessStatus.FEILET);
        prosessinstans.setType(ProsessType.MOTTAK_SED);
        prosessinstans.setSistFullførtSteg(null);
        prosessinstans.setRegistrertDato(LocalDateTime.MIN);
        prosessinstans.setEndretDato(endretDato);
        return prosessinstans;
    }
}
