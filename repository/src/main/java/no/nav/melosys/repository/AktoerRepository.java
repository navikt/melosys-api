package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AktoerRepository extends JpaRepository<Aktoer, Long> {
    List<Aktoer> findByFagsakAndFullmakterIsNotEmpty(Fagsak fagsak);

    List<Aktoer> findByFagsak(Fagsak fagsak);

    List<Aktoer> findByFagsakAndRolle(Fagsak fagsak, Aktoersroller aktoersroller);

    void deleteAllByFagsakAndRolle(Fagsak fagsak, Aktoersroller aktoersroller);

    void deleteByAktørId(String aktørId);
}
