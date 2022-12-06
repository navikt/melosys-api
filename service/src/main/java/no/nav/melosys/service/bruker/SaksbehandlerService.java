package no.nav.melosys.service.bruker;

import java.util.Objects;
import java.util.Optional;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerService {

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
}
