package no.nav.melosys.service.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.Saksbehandler;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerService {
    private static final String MELOSYS_BRUKERNAVN = "MELOSYS";

    private final LdapService ldapService;
    private final String melosysAdGruppe;

    private final Map<String, String> identTilNavnCache = new HashMap<>();

    public SaksbehandlerService(LdapService ldapService, @Value("${melosys.security.melosys_ad_group}") String melosysAdGruppe) {
        this.ldapService = ldapService;
        this.melosysAdGruppe = melosysAdGruppe;
    }

    public boolean harTilgangTilMelosys() throws TekniskException, IkkeFunnetException {
        return harTilgangTilMelosys(hentBrukerinformasjon());
    }

    public boolean harTilgangTilMelosys(Saksbehandler saksbehandler) {
        return saksbehandler.getGrupper().stream()
            .anyMatch(group -> group.equalsIgnoreCase(melosysAdGruppe));
    }

    public Saksbehandler hentBrukerinformasjon() throws TekniskException, IkkeFunnetException {
        return hentBrukerinformasjon(SubjectHandler.getInstance().getUserID());
    }

    private Saksbehandler hentBrukerinformasjon(String ident) throws TekniskException, IkkeFunnetException {
        return finnBrukerinformasjon(ident).orElseThrow(() -> new IkkeFunnetException("Finner ikke ident " + ident));
    }

    private Optional<Saksbehandler> finnBrukerinformasjon(String ident) throws TekniskException {
        return ldapService.finnBrukerinformasjon(ident)
            .map(l -> new Saksbehandler(ident, l.getDisplayName(), l.getGroups()));
    }

    public Optional<String> finnNavnForIdent(String ident) throws TekniskException {
        if (MELOSYS_BRUKERNAVN.equalsIgnoreCase(ident)) {
            return Optional.of(MELOSYS_BRUKERNAVN);
        }

        if (identTilNavnCache.containsKey(ident)) {
            return Optional.of(identTilNavnCache.get(ident));
        }

        Optional<String> navnForIdent = finnBrukerinformasjon(ident).map(Saksbehandler::getNavn);
        navnForIdent.ifPresent(navn -> identTilNavnCache.put(ident, navn));
        return navnForIdent;

    }

    public String hentNavnForIdent(String ident) throws IkkeFunnetException, TekniskException {
        return finnNavnForIdent(ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke navn for ident " + ident));
    }
}
