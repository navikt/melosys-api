package no.nav.melosys.service.brev;

import java.util.List;

import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.repository.UtkastBrevRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class BrevUtkastServiceTest {

    private final long BEHANDLING_ID = 1L;

    @Mock
    private UtkastBrevRepository utkastBrevRepository;

    private final UtkastBrevService brevUtkastService = new UtkastBrevService(utkastBrevRepository);

    @Test
    void hentUtkast_returnererAlleUtkastForBehandling() {
        when(utkastBrevRepository.findAllByBehandlingIDOrderByLagringsdatoDesc(BEHANDLING_ID)).thenReturn(List.of(lagUtkast(BEHANDLING_ID), lagUtkast(BEHANDLING_ID)));

        var utkast = brevUtkastService.hentUtkast(BEHANDLING_ID);

        assertEquals(2, utkast.size());
    }

    private UtkastBrev lagUtkast(long behandlingID) {
        var utkast = new UtkastBrev();
        utkast.setBehandlingID(behandlingID);
        return utkast;
    }
}
