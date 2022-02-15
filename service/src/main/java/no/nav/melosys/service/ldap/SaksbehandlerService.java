package no.nav.melosys.service.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import no.nav.melosys.domain.Saksbehandler;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.ldap.LdapService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerService {
    private final LdapService ldapService;
    private final String melosysAdGruppe;

    private final Map<String, String> identTilNavnCache = new HashMap<>();

    public SaksbehandlerService(LdapService ldapService, @Value("${melosys.security.melosys_ad_group}") String melosysAdGruppe) {
        this.ldapService = ldapService;
        this.melosysAdGruppe = melosysAdGruppe;
    }

    public boolean harTilgangTilMelosys() {
        return harTilgangTilMelosys(hentBrukerinformasjon());
    }

    public boolean harTilgangTilMelosys(Saksbehandler saksbehandler) {
        return saksbehandler.getGrupper().stream()
            .anyMatch(group -> group.equalsIgnoreCase(melosysAdGruppe));
    }

    public Saksbehandler hentBrukerinformasjon() {
        return hentBrukerinformasjon(SubjectHandler.getInstance().getUserID());
    }

    private Saksbehandler hentBrukerinformasjon(String ident) {
        return finnBrukerinformasjon(ident).orElseThrow(() -> new IkkeFunnetException("Finner ikke ident " + ident));
    }

    private Optional<Saksbehandler> finnBrukerinformasjon(String ident) {
        return ldapService.finnBrukerinformasjon(ident)
            .map(l -> new Saksbehandler(ident, formatterSaksbehandlerNavn(l.getDisplayName()), l.getGroups()));
    }

    public Optional<String> finnNavnForIdent(String ident) {
        if (identTilNavnCache.containsKey(ident)) {
            return Optional.of(identTilNavnCache.get(ident));
        }

        Optional<String> navnForIdent = finnBrukerinformasjon(ident).map(Saksbehandler::getNavn);
        navnForIdent.ifPresent(navn -> identTilNavnCache.put(ident, navn));
        return navnForIdent;

    }

    public String hentNavnForIdent(String ident) {
        return finnNavnForIdent(ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke navn for ident " + ident));
    }

    private String formatterSaksbehandlerNavn(String navn) {
        if (navn != null && navn.contains(",")) {
            return Navn.navnEtternavnSist(navn);
        }
        return navn;
    }
}
