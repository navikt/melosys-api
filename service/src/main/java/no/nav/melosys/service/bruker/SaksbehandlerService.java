package no.nav.melosys.service.bruker;

import java.util.Optional;

import io.getunleash.Unleash;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.azuread.AzureAdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SaksbehandlerService {

    private static final Logger log = LoggerFactory.getLogger(SaksbehandlerService.class);
    private AzureAdService azureAdService;

    public SaksbehandlerService(AzureAdService azureAdService) {
        this.azureAdService = azureAdService;
    }

    public Optional<String> finnNavnForIdent(String ident) {
        return Optional.ofNullable(azureAdService.hentSaksbehandlerNavn(ident));
    }

    public String hentNavnForIdent(String ident) {
        return finnNavnForIdent(ident)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke saksbehandler navn for ident " + ident));
    }
}
