package no.nav.melosys.service.abac;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.sikkerhet.abac.Pep;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

public final class TilgangTest {

    @Test
    public final void sjekkIkkeEksisterendeBehandlingKasterIkkeFunnet() throws Exception {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        Pep pep = mock(Pep.class);
        Tilgang instans = new Tilgang(behandlingRepository, pep);
        Throwable unntak = catchThrowable(() -> instans.sjekk(1));
        assertThat(unntak).isInstanceOf(IkkeFunnetException.class)
                .hasMessageContaining("ikke")
                .hasMessageContaining("finne")
                .hasMessageContaining("id 1");
    }

}
