package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.Person;
import no.nav.melosys.repository.PersonRepository;

@Service("eksempel")
@Transactional(readOnly = true)
public class EksempelServiceImpl implements EksempelService {

    @Autowired
    PersonRepository personRepository;

    public List<Person> findAll() {
        List<Person> list = new ArrayList<Person>();
        personRepository.findAll().forEach(list::add);
        return list;
    }

    @Override
    public List<Person> findByDate(Date date) {
        return personRepository.findByDate(date);
    }

    @Override
    public List<Person> findByEmail(String email) {
        return personRepository.findByEmail(email);
    }

    @Override
    public List<Person> findByNavn(String navn) {
        return personRepository.findByNavn(navn);
    }

    @Override
    @Transactional
    public Person addPerson(Person person) {
        return personRepository.save(person);
    }

    @Override
    @Transactional
    public Person updatePerson(Long id, Person updatedPerson) {
        return personRepository.save(updatedPerson);
    }

    @Override
    @Transactional
    public void deletePerson(Long id) {
        personRepository.delete(id);
    }

}
