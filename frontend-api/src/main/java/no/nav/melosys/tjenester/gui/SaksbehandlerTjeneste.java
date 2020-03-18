package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.freg.abac.core.annotation.Abac;
import no.nav.freg.abac.core.dto.response.Decision;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.abac.PepImpl;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.InnloggetBrukerDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Value;
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

    private final String melosysAdGruppe;

    private final LdapBrukeroppslag ldapBrukeroppslag;

    public SaksbehandlerTjeneste(LdapBrukeroppslag ldapBrukeroppslag,
                                 @Value("${melosys.security.melosys_ad_group}") String melosysAdGruppe) {
        this.ldapBrukeroppslag = ldapBrukeroppslag;
        this.melosysAdGruppe = melosysAdGruppe;
    }

    @GetMapping
    @Abac(bias = Decision.DENY, actions = @Abac.Attr(key = ACTION_ID, value = PepImpl.READ))
    @ApiOperation(
        value = "Returnerer fullt navn for ident",
        notes = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging."),
        response = InnloggetBrukerDto.class)
    public InnloggetBrukerDto innloggetBruker() throws TekniskException, SikkerhetsbegrensningException {
        String ident = SubjectHandler.getInstance().getUserID();

        LdapBruker ldapBruker;
        ldapBruker = ldapBrukeroppslag.hentBrukerinformasjon(ident);

        if (ldapBruker != null && !brukerErMedlemAvMelosysGruppe(ldapBruker)) {
            throw new SikkerhetsbegrensningException("Brukeren er ikke medlem av riktig AD-gruppe");
        }

        String navn = ldapBruker != null ? ldapBruker.getDisplayName() : "FEIL";

        return new InnloggetBrukerDto(ident, navn);
    }

    private boolean brukerErMedlemAvMelosysGruppe(LdapBruker ldapBruker) {
        return ldapBruker.getGroups().stream()
            .anyMatch(group -> group.equalsIgnoreCase(melosysAdGruppe));
    }
}