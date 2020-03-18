package no.nav.melosys.service.ldap;

import no.nav.melosys.domain.MelosysBruker;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
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

    public boolean harTilgangTilMelosys() throws TekniskException {
        return harTilgangTilMelosys(hentBrukerinformasjon());
    }

    public boolean harTilgangTilMelosys(MelosysBruker melosysBruker) {
        return melosysBruker.getGrupper().stream()
            .anyMatch(group -> group.equalsIgnoreCase(melosysAdGruppe));
    }

    public MelosysBruker hentBrukerinformasjon() throws TekniskException {
        return hentBrukerinformasjon(SpringSubjectHandler.getInstance().getUserID());
    }

    private MelosysBruker hentBrukerinformasjon(String ident) throws TekniskException {
        LdapBruker ldapBruker = brukeroppslag.hentBrukerinformasjon(ident);
        return new MelosysBruker(ident, ldapBruker.getDisplayName(), ldapBruker.getGroups());
    }
}
