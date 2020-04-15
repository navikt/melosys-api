package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.registeropplysninger.RegisterOppslagService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
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
    @ApiOperation(value = "Henter en organisasjon fra Enhetsregisteret.", response = OrganisasjonDokument.class)
    public ResponseEntity hentOrganisasjon(@PathVariable("orgnr") String orgnummer)
        throws SikkerhetsbegrensningException, IntegrasjonException {
        if (orgnummer == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        OrganisasjonDokument organisasjonDokument;
        try {
            organisasjonDokument = registerOppslag.hentOrganisasjon(orgnummer);
        } catch (IkkeFunnetException e) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.ok(organisasjonDokument);
    }
}
