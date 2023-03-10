package no.nav.melosys.service.bruker;

import java.util.Optional;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.azuread.AzureAdService;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerService {
    private AzureAdService azureAdService;

    public SaksbehandlerService(AzureAdService azureAdService) {
        this.azureAdService = azureAdService;
    }

    public Optional<String> finnNavnForIdent(String ident) {
        String saksbehandlerNavn = azureAdService.hentSaksbehandlerNavn(ident);
        return Optional.of(saksbehandlerNavn);
    }

    public String hentNavnForIdent(String ident) {
        return finnNavnForIdent(ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke saksbehandler navn for ident " + ident));
    }
}
