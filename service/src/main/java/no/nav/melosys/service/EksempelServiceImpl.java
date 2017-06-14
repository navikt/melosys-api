package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.Bruker;
import no.nav.melosys.repository.BrukerRepository;

@Service("eksempel")
@Transactional(readOnly = true)
public class EksempelServiceImpl implements EksempelService {

    @Autowired
    BrukerRepository brukerRepository;

    @Override
    public List<Bruker> findAll() {
        List<Bruker> list = new ArrayList<>();
        brukerRepository.findAll().forEach(list::add);
        return list;
    }

    @Override
    public List<Bruker> findByNavn(String navn) {
        return brukerRepository.findByNavn(navn);
    }

    @Override
    @Transactional
    public Bruker addBruker(Bruker person) {
        return brukerRepository.save(person);
    }

    @Override
    @Transactional
    public Bruker updateBruker(Long id, Bruker updatedPerson) {
        return brukerRepository.save(updatedPerson);
    }

    @Override
    @Transactional
    public void deletePerson(Long id) {
        brukerRepository.delete(id);
    }

}
