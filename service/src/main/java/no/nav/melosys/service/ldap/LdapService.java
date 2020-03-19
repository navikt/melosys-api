package no.nav.melosys.service.ldap;

import java.util.Optional;

import no.nav.melosys.domain.MelosysBruker;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LdapService {

    private final LdapBrukeroppslag brukeroppslag;
    private final String melosysAdGruppe;

    public LdapService(LdapBrukeroppslag brukeroppslag, @Value("${melosys.security.melosys_ad_group}") String melosysAdGruppe) {
        this.brukeroppslag = brukeroppslag;
        this.melosysAdGruppe = melosysAdGruppe;
    }

    public boolean harTilgangTilMelosys() throws TekniskException, IkkeFunnetException {
        return harTilgangTilMelosys(hentBrukerinformasjon());
    }

    public boolean harTilgangTilMelosys(MelosysBruker melosysBruker) {
        return melosysBruker.getGrupper().stream()
            .anyMatch(group -> group.equalsIgnoreCase(melosysAdGruppe));
    }

    public MelosysBruker hentBrukerinformasjon() throws TekniskException, IkkeFunnetException {
        return hentBrukerinformasjon(SpringSubjectHandler.getInstance().getUserID());
    }

    private MelosysBruker hentBrukerinformasjon(String ident) throws TekniskException, IkkeFunnetException {
        return finnBrukerinformasjon(ident).orElseThrow(() -> new IkkeFunnetException("Finner ikke ident" + ident));
    }

    private Optional<MelosysBruker> finnBrukerinformasjon(String ident) throws TekniskException {
        return brukeroppslag.finnBrukerinformasjon(ident)
            .map(l -> new MelosysBruker(ident, l.getDisplayName(), l.getGroups()));
    }
}
