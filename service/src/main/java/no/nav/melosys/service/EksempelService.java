package no.nav.melosys.service;

import java.util.List;

import no.nav.melosys.domain.Bruker;

public interface EksempelService {

    List<Bruker> findAll();

    List<Bruker> findByNavn(String name);

    Bruker addBruker(Bruker bruker);

    Bruker updateBruker(Long id, Bruker bruker);

    void deletePerson(Long id);
}
