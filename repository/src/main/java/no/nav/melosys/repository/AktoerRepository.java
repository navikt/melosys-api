package no.nav.melosys.repository;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AktoerRepository extends JpaRepository<Aktoer, Long> {

    void deleteAllByFagsakAndRolle(Fagsak fagsak, Aktoersroller aktoersroller);

    void deleteByAktørId(String aktørId);
}
