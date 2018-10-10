package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.freg.abac.core.annotation.Abac;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.abac.PepImpl;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.InnloggetBrukerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.abac.xacml.StandardAttributter.ACTION_ID;

@Api(tags = {"saksbehandler"})
@Path("/saksbehandler")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class SaksbehandlerTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SaksbehandlerTjeneste.class);

    @GET
    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    @ApiOperation(
        value = "Returnerer fullt navn for ident",
        notes = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging."),
        response = InnloggetBrukerDto.class)
    public InnloggetBrukerDto innloggetBruker() {
        String ident = SubjectHandler.getInstance().getUserID();

        LdapBruker ldapBruker;
        try {
            ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            throw new InternalServerErrorException("Intern feil");
        }

        String navn = ldapBruker != null ? ldapBruker.getDisplayName() : "FEIL";

        return new InnloggetBrukerDto(ident, navn);
    }

}