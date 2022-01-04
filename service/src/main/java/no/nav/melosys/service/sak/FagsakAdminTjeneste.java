package no.nav.melosys.service.sak;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.service.AdminTjeneste;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Unprotected
@RestController
@RequestMapping("/admin/fagsaker")
public class FagsakAdminTjeneste implements AdminTjeneste {
    private final String apiKey;
    private final FagsakService fagsakService;

    public FagsakAdminTjeneste(@Value("${Melosys-admin.apikey}") String apiKey, FagsakService fagsakService) {
        this.apiKey = apiKey;
        this.fagsakService = fagsakService;
    }

    @PostMapping("/{saksnummer}/henlegg-bortfalt")
    public ResponseEntity<Void> henleggFagsakSomBortfalt(@PathVariable String saksnummer,
                                              @RequestHeader(API_KEY_HEADER) String apiKey) {
        validerApikey(apiKey);

        final Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        fagsakService.henleggSomBortfalt(fagsak);
        return ResponseEntity.ok().build();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
