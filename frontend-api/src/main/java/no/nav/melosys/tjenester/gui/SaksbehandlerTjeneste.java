package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.freg.abac.core.annotation.Abac;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.melosys.domain.Saksbehandler;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.sikkerhet.abac.PepImpl;
import no.nav.melosys.tjenester.gui.dto.InnloggetBrukerDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.abac.xacml.StandardAttributter.ACTION_ID;

@Protected
@RestController
@RequestMapping("/saksbehandler")
@Api(tags = {"saksbehandler"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SaksbehandlerTjeneste {

    private final SaksbehandlerService saksbehandlerService;

    public SaksbehandlerTjeneste(SaksbehandlerService saksbehandlerService) {
        this.saksbehandlerService = saksbehandlerService;
    }

    @GetMapping
    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    @ApiOperation(
        value = "Returnerer fullt navn for ident",
        notes = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging."),
        response = InnloggetBrukerDto.class)
    public InnloggetBrukerDto innloggetBruker() {
        Saksbehandler saksbehandler = saksbehandlerService.hentBrukerinformasjon();

        if (!saksbehandlerService.harTilgangTilMelosys(saksbehandler)) {
            throw new SikkerhetsbegrensningException("Brukeren er ikke medlem av riktig AD-gruppe");
        }

        return new InnloggetBrukerDto(saksbehandler.getIdent(), saksbehandler.getNavn());
    }
}
