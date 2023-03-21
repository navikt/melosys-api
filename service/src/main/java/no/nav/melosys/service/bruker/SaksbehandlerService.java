package no.nav.melosys.service.bruker;

import java.util.Objects;
import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.azuread.AzureAdService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerService {
    private AzureAdService azureAdService;

    private Unleash unleash;

    public SaksbehandlerService(AzureAdService azureAdService, Unleash unleash) {
        this.azureAdService = azureAdService;
        this.unleash = unleash;
    }

    public Optional<String> finnNavnForIdentFraToken(String ident) {
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

    public Optional<String> finnNavnForIdentFraAzure(String ident) {
        return Optional.of(azureAdService.hentSaksbehandlerNavn(ident));
    }

    public Optional<String> finnNavnForIdent(String ident) {
        var saksbehandlerNavnFraToken = finnNavnForIdentFraToken(ident);

        if (!unleash.isEnabled("melosys.azure_graph")) {
            return saksbehandlerNavnFraToken;
        }

        if (saksbehandlerNavnFraToken.isPresent()) {
            return saksbehandlerNavnFraToken;
        } else {
            return finnNavnForIdentFraAzure(ident);
        }
    }

    public String hentNavnForIdent(String ident) {
        return finnNavnForIdent(ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke saksbehandler navn for ident " + ident));
    }
}
