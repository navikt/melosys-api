package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.exception.IntegrasjonException;

public interface BehandlingskjedeConsumer {

    List<SakDto> finnSakOgBehandlingskjedeListeResponse(String aktørId, LocalDate tidspunkt) throws IntegrasjonException;
}
