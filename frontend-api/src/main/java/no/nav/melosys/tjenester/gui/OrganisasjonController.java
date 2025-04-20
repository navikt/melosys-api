package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/organisasjoner")
@Tag(name = "organisasjoner")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OrganisasjonController {
    private final OrganisasjonOppslagService registerOppslag;

    public OrganisasjonController(OrganisasjonOppslagService registerOppslag) {
        this.registerOppslag = registerOppslag;
    }

    @GetMapping("{orgnr}")
    @JsonView(DokumentView.FrontendApi.class)
    @Operation(summary = "Henter en organisasjon fra Enhetsregisteret.")
    public ResponseEntity<OrganisasjonDokument> hentOrganisasjon(@PathVariable("orgnr") String orgnummer) {
        return ResponseEntity.ok(registerOppslag.hentOrganisasjon(orgnummer));
    }
}
