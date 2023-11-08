package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.brev.BrevAdresse;
import no.nav.melosys.service.brev.TilBrevAdresseService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/adresser")
@Api(tags = {"adresser"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AdresseTjeneste {
    private final TilBrevAdresseService tilBrevAdresseService;

    public AdresseTjeneste(TilBrevAdresseService tilBrevAdresseService) {
        this.tilBrevAdresseService = tilBrevAdresseService;
    }

    @GetMapping()
    @ApiOperation(
        value = "Henter adresse til person eller adresse til organisasjon, men tar ikke hensyn til eventuelle kontaktopplysninger og fullmektige",
        response = BrevAdresse.class)
    public ResponseEntity<BrevAdresse> hentAdresseTilPersonEllerOrganisasjon(@RequestParam(name = "personIdent", required = false) String personIdent,
                                                                             @RequestParam(name = "orgnr", required = false) String organisasjonsnummer) {
        if (personIdent == null && organisasjonsnummer == null) {
            throw new FunksjonellException("Send inn enten personIdent eller organisasjonsnummer til personen/organisasjonen du vil finne adresse til");
        }
        return ResponseEntity.ok(tilBrevAdresseService.tilBrevAdresse(personIdent, organisasjonsnummer));
    }
}
