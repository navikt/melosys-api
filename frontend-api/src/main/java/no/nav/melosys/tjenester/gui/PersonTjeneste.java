package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
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
    private final Aksesskontroll aksesskontroll;

    @Autowired
    public PersonTjeneste(RegisterOppslagService registerOppslag, Aksesskontroll aksesskontroll) {
        this.registerOppslag = registerOppslag;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("{fnr}")
    @JsonView(DokumentView.FrontendApi.class)
    @ApiOperation(value = "Henter en person fra TPS.", response = PersonDokument.class)
    public ResponseEntity getPerson(@PathVariable("fnr") String folkeregisterIdent) {
        if (folkeregisterIdent == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        PersonDokument personDokument = registerOppslag.hentPerson(folkeregisterIdent);
        aksesskontroll.autoriserFolkeregisterIdent(folkeregisterIdent);
        return ResponseEntity.ok(new PersonDto(personDokument));
    }
}
