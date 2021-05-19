package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import no.nav.melosys.tjenester.gui.dto.OrganisasjonDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
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
@Api(tags = {"organisasjoner"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OrganisasjonTjeneste {
    private final RegisterOppslagService registerOppslag;

    @Autowired
    public OrganisasjonTjeneste(RegisterOppslagService registerOppslag) {
        this.registerOppslag = registerOppslag;
    }

    @GetMapping("{orgnr}")
    @JsonView(DokumentView.FrontendApi.class)
    @ApiOperation(value = "Henter en organisasjon fra Enhetsregisteret.", response = OrganisasjonDto.class)
    public ResponseEntity<OrganisasjonDokument> hentOrganisasjon(@PathVariable("orgnr") String orgnummer) {
        return ResponseEntity.ok(registerOppslag.hentOrganisasjon(orgnummer));
    }
}
