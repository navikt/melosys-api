package no.nav.melosys.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import no.nav.melosys.domain.PersonEks;
import no.nav.melosys.service.EksempelService;

@Deprecated
@RestController
@RequestMapping(value = "/person", produces = "application/json")
public class EksempelController {

    @Autowired
    EksempelService eksempelService;

    @RequestMapping(value = "/{navn}", method = RequestMethod.GET)
    public List<PersonEks> findByNavn(@PathVariable String navn) {
        return eksempelService.findByNavn(navn);
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<PersonEks> findAll() {
        return eksempelService.findAll();
    }

    @RequestMapping(method = RequestMethod.POST)
    public PersonEks createPerson(@RequestBody PersonEks person) {
        return eksempelService.addPerson(person);
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.POST)
    public PersonEks updatePerson(@PathVariable("id") Long id, @RequestBody PersonEks updatedPerson) {
        return eksempelService.updatePerson(id, updatedPerson);
    }

    @RequestMapping(path = "/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deletePerson(@PathVariable("id") Long id) {
        eksempelService.deletePerson(id);
    }

}
