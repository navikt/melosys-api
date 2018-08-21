package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import org.springframework.data.repository.CrudRepository;

public interface OppgaveTilbakeleggingRepository extends CrudRepository<OppgaveTilbakelegging, Long> {

    List<OppgaveTilbakelegging> findBySaksbehandlerIdAndOppgaveId(String ident, String oppgaveId);
}
