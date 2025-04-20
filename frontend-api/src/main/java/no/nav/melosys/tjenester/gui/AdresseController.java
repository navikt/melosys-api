package no.nav.melosys.tjenester.gui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "adresser")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AdresseController {
    private final TilBrevAdresseService tilBrevAdresseService;

    public AdresseController(TilBrevAdresseService tilBrevAdresseService) {
        this.tilBrevAdresseService = tilBrevAdresseService;
    }

    @GetMapping()
    @Operation(summary = "Henter adresse til person eller adresse til organisasjon, men tar ikke hensyn til eventuelle kontaktopplysninger og fullmektige")
    public ResponseEntity<BrevAdresse> hentAdresseTilPersonEllerOrganisasjon(@RequestParam(name = "personIdent", required = false) String personIdent,
                                                                             @RequestParam(name = "orgnr", required = false) String organisasjonsnummer) {
        if (personIdent == null && organisasjonsnummer == null) {
            throw new FunksjonellException("Send inn enten personIdent eller organisasjonsnummer til personen/organisasjonen du vil finne adresse til");
        }
        return ResponseEntity.ok(tilBrevAdresseService.tilBrevAdresse(personIdent, organisasjonsnummer));
    }
}
