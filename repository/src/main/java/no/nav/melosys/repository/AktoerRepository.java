package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AktoerRepository extends JpaRepository<Aktoer, Long> {

    Optional<Aktoer> findByFagsakAndRolleAndRepresenterer(Fagsak fagsak, Aktoersroller aktoersroller, Representerer representerer);

    void deleteAllByFagsakAndRolle(Fagsak fagsak, Aktoersroller aktoersroller);

    void deleteByAktørId(String aktørId);
}
