package no.nav.melosys.saksflyt.kontroll;

import java.time.LocalDateTime;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.impl.BehandleProsessinstansDelegate;
import no.nav.melosys.saksflyt.kontroll.dto.HentProsessinstansDto;
import no.nav.melosys.saksflyt.kontroll.dto.RestartProsessinstanserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProsessinstansAdminTjenesteTest {

    @Mock
    private ProsessinstansRepository prosessinstansRepository;
    @Mock
    private BehandleProsessinstansDelegate behandleProsessinstansDelegate;
    private final String apiKey = "dummy";

    private ProsessinstansAdminTjeneste prosessinstansAdminTjeneste;

    @BeforeEach
    public void setup() {
        prosessinstansAdminTjeneste = new ProsessinstansAdminTjeneste(behandleProsessinstansDelegate, prosessinstansRepository, apiKey);
    }

    @Test
    void hentFeiledeProsessinstanser_enProsessinstansFlereHendelser_viserFeilmeldingSisteHendelse() {
        final var sisteFeilmelding = "siste feilmelding";
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.getHendelser().add(new ProsessinstansHendelse(prosessinstans, LocalDateTime.now().minusDays(1), null, null, "første"));
        prosessinstans.getHendelser().add(new ProsessinstansHendelse(prosessinstans, LocalDateTime.now(), null, null, sisteFeilmelding));

        when(prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET))
            .thenReturn(singletonList(prosessinstans));

        var response = prosessinstansAdminTjeneste.hentFeiledeProsessinstanser(apiKey);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var body = response.getBody();
        assertThat(body)
            .flatExtracting(HentProsessinstansDto::getId, HentProsessinstansDto::getBehandlingId, HentProsessinstansDto::getEndretDato,
                HentProsessinstansDto::getProsessType, HentProsessinstansDto::getSistFullførtSteg,
                HentProsessinstansDto::getSisteFeilmelding)
            .containsExactly(prosessinstans.getId(), prosessinstans.getBehandling().getId(), prosessinstans.getEndretDato(),
                prosessinstans.getType().getKode(), prosessinstans.getSistFullførtSteg().getKode(),
                sisteFeilmelding);
    }

    @Test
    void restartAlleFeiledeProsessinstanser_treFeilet_restarterIRekkefølge() {
        Prosessinstans tidligstFeilet = lagProsessinstans(LocalDateTime.now().minusDays(3));
        Prosessinstans nestTidligstFeilet = lagProsessinstans(LocalDateTime.now().minusDays(2));
        Prosessinstans senestFeilet = lagProsessinstans(LocalDateTime.now());

        when(prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET))
            .thenReturn(Set.of(tidligstFeilet, nestTidligstFeilet, senestFeilet));
        when(prosessinstansRepository.saveAll(any())).thenAnswer(a -> new ArrayList<>((Collection<?>) a.getArgument(0)));

        prosessinstansAdminTjeneste.restartAlleFeiledeProsessinstanser(apiKey);

        InOrder inOrder = Mockito.inOrder(behandleProsessinstansDelegate);
        inOrder.verify(behandleProsessinstansDelegate).behandleProsessinstans(tidligstFeilet);
        inOrder.verify(behandleProsessinstansDelegate).behandleProsessinstans(nestTidligstFeilet);
        inOrder.verify(behandleProsessinstansDelegate).behandleProsessinstans(senestFeilet);
    }

    @Test
    void restartProsessinstans_prosessinstansHarStatusKlar_kasterFeil() {
        Prosessinstans prosessinstans = lagProsessinstans();
        prosessinstans.setStatus(ProsessStatus.KLAR);
        UUID uuid = prosessinstans.getId();

        when(prosessinstansRepository.findAllById(anyList()))
            .thenReturn(singletonList(prosessinstans));

        final var request = new RestartProsessinstanserRequest(singletonList(uuid));
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> prosessinstansAdminTjeneste.restartProsessinstans(apiKey, request))
            .withMessageContaining("har status");
    }

    @Test
    void restartProsessinstans_prosessinstansHarStatusFeilet_blirRestartet() {
        Prosessinstans prosessinstans = lagProsessinstans();
        UUID uuid = prosessinstans.getId();

        when(prosessinstansRepository.findAllById(anyList()))
            .thenReturn(singletonList(prosessinstans));
        when(prosessinstansRepository.saveAll(any())).then(a -> a.getArgument(0, List.class));

        prosessinstansAdminTjeneste.restartProsessinstans(apiKey, new RestartProsessinstanserRequest(singletonList(uuid)));

        assertThat(prosessinstans.getStatus()).isEqualTo(ProsessStatus.RESTARTET);
        verify(prosessinstansRepository).saveAll(singletonList(prosessinstans));
        verify(behandleProsessinstansDelegate).behandleProsessinstans(prosessinstans);
    }

    @Test
    void hentFeiledeProsessinstanser_feilApiKeyOppgitt_forventForbidden() {
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> prosessinstansAdminTjeneste.hentFeiledeProsessinstanser("Dum dummy"))
            .withMessageContaining("apikey");
    }

    @Test
    void restartProsessinstans_feilApiKeyOppgitt_forventForbidden() {
        final var request = new RestartProsessinstanserRequest(List.of());
        assertThatExceptionOfType(SikkerhetsbegrensningException.class)
            .isThrownBy(() -> prosessinstansAdminTjeneste.restartProsessinstans("Dumdum", request))
            .withMessageContaining("apikey");
    }


    private Prosessinstans lagProsessinstans() {
        return lagProsessinstans(LocalDateTime.now());
    }

    private Prosessinstans lagProsessinstans(LocalDateTime registrertDato) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setId(UUID.randomUUID());
        prosessinstans.setBehandling(lagBehandling());
        prosessinstans.setStatus(ProsessStatus.FEILET);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
        prosessinstans.setSistFullførtSteg(ProsessSteg.AVKLAR_ARBEIDSGIVER);
        prosessinstans.setRegistrertDato(registrertDato);
        prosessinstans.setEndretDato(registrertDato);
        return prosessinstans;
    }

    private Behandling lagBehandling() {
        var behandling = new Behandling();
        behandling.setId(1L);
        return behandling;
    }
}
