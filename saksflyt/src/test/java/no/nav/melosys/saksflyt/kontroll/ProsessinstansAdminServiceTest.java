package no.nav.melosys.saksflyt.kontroll;

import java.time.LocalDateTime;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.ProsessinstansBehandlerDelegate;
import no.nav.melosys.saksflyt.ProsessinstansRepository;
import no.nav.melosys.saksflyt.kontroll.dto.HentProsessinstansDto;
import no.nav.melosys.saksflytapi.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProsessinstansAdminServiceTest {

    // Viktig at forrige og current er steg som kommer rett etter hverandre i samme prosess(type)
    private static final ProsessType PROSESS_TYPE = ProsessType.JFR_NY_SAK_BRUKER;
    private static final ProsessSteg FORSTE_PROSSESS_STEG = ProsessSteg.OPPRETT_SAK_OG_BEH;
    private static final ProsessSteg FORRIGE_PROSSESS_STEG = ProsessSteg.OPPRETT_MOTTATTEOPPLYSNINGER;
    private static final ProsessSteg CURRENT_PROSESS_STEG = ProsessSteg.OPPRETT_ARKIVSAK;

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private ProsessinstansBehandlerDelegate prosessinstansBehandlerDelegate;

    private ProsessinstansAdminService prosessinstansAdminService;

    @BeforeEach
    public void setup() {
        prosessinstansAdminService = new ProsessinstansAdminService(prosessinstansBehandlerDelegate, prosessinstansRepository);
    }

    @Test
    void hentFeiledeProsessinstanser_enProsessinstansFlereHendelser_viserFeilmeldingSisteHendelse() {
        final var sisteFeilmelding = "siste feilmelding";
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.getHendelser().add(new ProsessinstansHendelse(prosessinstans, LocalDateTime.now().minusDays(1), null, null, "første"));
        prosessinstans.getHendelser().add(new ProsessinstansHendelse(prosessinstans, LocalDateTime.now(), null, null, sisteFeilmelding));

        when(prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET))
            .thenReturn(singletonList(prosessinstans));

        var prosessinstanser = prosessinstansAdminService.hentFeiledeProsessinstanser();

        assertThat(prosessinstanser)
            .flatExtracting(HentProsessinstansDto::id,
                HentProsessinstansDto::behandlingId, HentProsessinstansDto::saksnummer,
                HentProsessinstansDto::endretDato, HentProsessinstansDto::registrertDato,
                HentProsessinstansDto::prosessType, HentProsessinstansDto::feiletSteg,
                HentProsessinstansDto::sisteFeilmelding, HentProsessinstansDto::correlationId)
            .containsExactly(prosessinstans.getId(),
                prosessinstans.getBehandling().getId(), prosessinstans.getBehandling().getFagsak().getSaksnummer(),
                prosessinstans.getEndretDato(), prosessinstans.getRegistrertDato(), prosessinstans.getType().getKode(),
                CURRENT_PROSESS_STEG.getKode(), sisteFeilmelding, prosessinstans.getData(ProsessDataKey.CORRELATION_ID_SAKSFLYT));
    }

    @Test
    void hentFeiletSteg_forrigeErNull_girForsteSteg() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans = prosessinstans.toBuilder()
            .medSistFullførtSteg(null)
            .build();

        when(prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET))
            .thenReturn(singletonList(prosessinstans));

        var prosessinstanser = prosessinstansAdminService.hentFeiledeProsessinstanser();

        assertThat(prosessinstanser.get(0).feiletSteg()).isEqualTo(FORSTE_PROSSESS_STEG.getKode());
    }

    @Test
    void restartAlleFeiledeProsessinstanser_treFeilet_restarterIRekkefølge() {
        Prosessinstans tidligstFeilet = lagProsessinstans(LocalDateTime.now().minusDays(3));
        Prosessinstans nestTidligstFeilet = lagProsessinstans(LocalDateTime.now().minusDays(2));
        Prosessinstans senestFeilet = lagProsessinstans(LocalDateTime.now());

        when(prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET))
            .thenReturn(Set.of(tidligstFeilet, nestTidligstFeilet, senestFeilet));
        when(prosessinstansRepository.saveAll(any())).thenAnswer(a -> new ArrayList<>((Collection<?>) a.getArgument(0)));

        prosessinstansAdminService.restartAlleFeiledeProsessinstanser();

        InOrder inOrder = Mockito.inOrder(prosessinstansBehandlerDelegate);
        inOrder.verify(prosessinstansBehandlerDelegate).behandleProsessinstans(tidligstFeilet);
        inOrder.verify(prosessinstansBehandlerDelegate).behandleProsessinstans(nestTidligstFeilet);
        inOrder.verify(prosessinstansBehandlerDelegate).behandleProsessinstans(senestFeilet);
    }

    @Test
    void restartProsessinstans_prosessinstansHarStatusFerdig_kanIkkeGjenstartes() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans = prosessinstans.toBuilder()
            .medStatus(ProsessStatus.FERDIG)
            .build();
        UUID uuid = prosessinstans.getId();

        when(prosessinstansRepository.findAllById(anyList()))
            .thenReturn(singletonList(prosessinstans));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> prosessinstansAdminService.restartProsessinstanser(singletonList(uuid)))
            .withMessageContaining("har status");
    }

    @Test
    void restartProsessinstans_prosessinstansErNyOgHarStatusKlar_kasterFeil() {
        Prosessinstans prosessinstans = lagProsessinstans(LocalDateTime.now());
        prosessinstans = prosessinstans.toBuilder()
            .medStatus(ProsessStatus.KLAR)
            .build();
        UUID uuid = prosessinstans.getId();

        when(prosessinstansRepository.findAllById(anyList()))
            .thenReturn(singletonList(prosessinstans));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> prosessinstansAdminService.restartProsessinstanser(singletonList(uuid)))
            .withMessageContaining("for mindre enn");
    }

    @Test
    void restartProsessinstans_prosessinstansErGammelOgHarStatusKlar_blirRestartet() {
        Prosessinstans prosessinstans = lagProsessinstans(LocalDateTime.now().minusDays(2));
        prosessinstans = prosessinstans.toBuilder()
            .medStatus(ProsessStatus.KLAR)
            .build();
        when(prosessinstansRepository.findAllById(Set.of(prosessinstans.getId()))).thenReturn(List.of(prosessinstans));

        prosessinstansAdminService.restartProsessinstanser(Set.of(prosessinstans.getId()));

        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.RESTARTET);
        verify(prosessinstansRepository).saveAll(singletonList(prosessinstans));
        verify(prosessinstansBehandlerDelegate).behandleProsessinstans(prosessinstans);
    }

    @Test
    void restartProsessinstans_prosessinstansHarStatusFeilet_blirRestartet() {
        Prosessinstans prosessinstans = lagProsessinstans();
        UUID uuid = prosessinstans.getId();

        when(prosessinstansRepository.findAllById(anyList()))
            .thenReturn(singletonList(prosessinstans));
        when(prosessinstansRepository.saveAll(any())).then(a -> a.getArgument(0, List.class));

        prosessinstansAdminService.restartProsessinstanser(singletonList(uuid));

        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.RESTARTET);
        verify(prosessinstansRepository).saveAll(singletonList(prosessinstans));
        verify(prosessinstansBehandlerDelegate).behandleProsessinstans(prosessinstans);
    }

    @Test
    void hoppOverStegProsessinstans_hopperTilNesteSteg() {
        var prosessinstans = lagProsessinstans();
        var uuid = prosessinstans.getId();

        when(prosessinstansRepository.findById(uuid)).thenReturn(Optional.of(prosessinstans));

        prosessinstansAdminService.hoppOverStegProsessinstans(uuid);

        assertThat(prosessinstans.getSistFullførtSteg()).isEqualTo(CURRENT_PROSESS_STEG);
        verify(prosessinstansRepository).save(prosessinstans);
    }

    private Prosessinstans lagProsessinstans() {
        return lagProsessinstans(LocalDateTime.now());
    }

    private Prosessinstans lagProsessinstans(LocalDateTime registrertDato) {
        return ProsessinstansTestFactory.builderWithDefaults()
            .medType(PROSESS_TYPE)
            .medStatus(ProsessStatus.FEILET)
            .medId(UUID.randomUUID())
            .medBehandling(lagBehandling())
            .medSistFullførtSteg(FORRIGE_PROSSESS_STEG)
            .medRegistrertDato(registrertDato)
            .medEndretDato(registrertDato)
            .medData(ProsessDataKey.CORRELATION_ID_SAKSFLYT, "correlation-id")
            .build();
    }

    private Behandling lagBehandling() {
        return BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medFagsak(FagsakTestFactory.lagFagsak())
            .build();
    }
}
