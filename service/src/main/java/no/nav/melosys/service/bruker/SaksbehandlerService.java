package no.nav.melosys.service.bruker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import no.nav.melosys.domain.Saksbehandler;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerService {
    private final String melosysAdGruppe;

    private final Map<String, String> identTilNavnCache = new HashMap<>();

    public SaksbehandlerService(@Value("${melosys.security.melosys_ad_group}") String melosysAdGruppe) {
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
        return new Saksbehandler(SubjectHandler.getInstance().getUserID(), SubjectHandler.getInstance().getUserName(), SubjectHandler.getInstance().getGroups());
    }

    public Optional<String> finnNavnForIdent(String ident) {
        // Midlertidig løsning før vi legger inn støtte for å slå dette opp i azure ad
        SubjectHandler instance = SubjectHandler.getInstance();
        String userID = instance.getUserID();
        if (userID != null) {
            if (!Objects.equals(ident, userID)) {
                return Optional.empty();
            }
            return Optional.of(instance.getUserName());
        }

        String saksbehandlerID = ThreadLocalAccessInfo.getSaksbehandler();
        if (ident.equals(saksbehandlerID)) {
            String saksbehandlerNavn = ThreadLocalAccessInfo.getSaksbehandlerNavn();
            return Optional.of(saksbehandlerNavn);
        }
        return Optional.empty();
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
