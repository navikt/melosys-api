package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/personer")
@Api(tags = {"personer"})
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class PersonTjeneste {
    private final RegisterOppslagService registerOppslag;
    private final TilgangService tilgangService;

    @Autowired
    public PersonTjeneste(RegisterOppslagService registerOppslag, TilgangService tilgangService) {
        this.registerOppslag = registerOppslag;
        this.tilgangService = tilgangService;
    }

    @GetMapping("{fnr}")
    @ApiOperation(value = "Henter en person fra TPS.", response = PersonDokument.class)
    public ResponseEntity getPerson(@PathVariable("fnr") String personnummer)
        throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        if (personnummer == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        PersonDokument personDokument = registerOppslag.hentPerson(personnummer);
        tilgangService.sjekkFnr(personnummer);
        return ResponseEntity.ok(new PersonDto(personDokument));
    }
}
