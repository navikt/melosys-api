package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.OppgaveTilbakelegging;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface OppgaveTilbakeleggingRepository extends CrudRepository<OppgaveTilbakelegging, Long> {

    @Query("select o from OppgaveTilbakelegging o where o.saksbehandlerId = :ident  and o.oppgaveId = :oppgaveId") //$NON-NLS-1$
    List<OppgaveTilbakelegging> findBySaksbehandlerAndOppgaveId(@Param("ident") String ident, @Param("oppgaveId") String oppgaveId);
}
