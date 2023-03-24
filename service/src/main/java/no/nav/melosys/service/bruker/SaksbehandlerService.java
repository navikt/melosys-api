package no.nav.melosys.service.bruker;

import java.util.Objects;
import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.azuread.AzureAdService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerService {

    private static final Logger log = LoggerFactory.getLogger(SaksbehandlerService.class);
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
            return Optional.of(Navn.navnEtternavnSist(saksbehandlerNavn));
        }
        return Optional.empty();
    }

    public Optional<String> finnNavnForIdentFraAzure(String ident) {
        return Optional.ofNullable(azureAdService.hentSaksbehandlerNavn(ident));
    }

    public Optional<String> finnNavnForIdent(String ident) {
        var saksbehandlerNavnFraToken = finnNavnForIdentFraToken(ident);

        if (!unleash.isEnabled("melosys.azure_graph")) {
            return saksbehandlerNavnFraToken;
        }

        if (saksbehandlerNavnFraToken.isPresent()) {
            log.warn("Saksbehandlers navn er tilgjengelig i token, men henter navn fra Graph API");
        }

        return finnNavnForIdentFraAzure(ident);
    }

    public String hentNavnForIdent(String ident) {
        return finnNavnForIdent(ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke saksbehandler navn for ident " + ident));
    }
}
