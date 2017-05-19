package no.nav.melosys.controller;

import no.nav.melosys.domain.Person;
import no.nav.melosys.service.EksempelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/person", produces = "application/json")
public class EksempelController {

    @Autowired
    EksempelService eksempelService;

    @RequestMapping(value = "/{navn}", method = RequestMethod.GET)
    public List<Person> findByNavn(@PathVariable String navn) {
        return eksempelService.findByNavn(navn);
    }

    @RequestMapping(method = RequestMethod.GET)
    public List<Person> findAll() {
        return eksempelService.findAll();
    }
}
