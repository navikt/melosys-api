package no.nav.melosys.service;

import no.nav.melosys.domain.Person;
import no.nav.melosys.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

}
