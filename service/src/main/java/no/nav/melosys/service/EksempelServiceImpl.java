package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.PersonEks;
import no.nav.melosys.repository.PersonRepository;

@Service("eksempel")
@Transactional(readOnly = true)
public class EksempelServiceImpl implements EksempelService {

    @Autowired
    PersonRepository personRepository;

    public List<PersonEks> findAll() {
        List<PersonEks> list = new ArrayList<PersonEks>();
        personRepository.findAll().forEach(list::add);
        return list;
    }

    @Override
    public List<PersonEks> findByDate(Date date) {
        return personRepository.findByDate(date);
    }

    @Override
    public List<PersonEks> findByEmail(String email) {
        return personRepository.findByEmail(email);
    }

    @Override
    public List<PersonEks> findByNavn(String navn) {
        return personRepository.findByNavn(navn);
    }

    @Override
    @Transactional
    public PersonEks addPerson(PersonEks person) {
        return personRepository.save(person);
    }

    @Override
    @Transactional
    public PersonEks updatePerson(Long id, PersonEks updatedPerson) {
        return personRepository.save(updatedPerson);
    }

    @Override
    @Transactional
    public void deletePerson(Long id) {
        personRepository.delete(id);
    }

}
