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
    private final HenleggFagsakService henleggFagsakService;

    public FagsakAdminTjeneste(@Value("${Melosys-admin.apikey}") String apiKey, FagsakService fagsakService,
                               HenleggFagsakService henleggFagsakService) {
        this.apiKey = apiKey;
        this.fagsakService = fagsakService;
        this.henleggFagsakService = henleggFagsakService;
    }

    @PutMapping("/{saksnummer}/henlegg-bortfalt")
    public ResponseEntity<Void> henleggFagsakSomBortfalt(@PathVariable String saksnummer,
                                              @RequestHeader(API_KEY_HEADER) String apiKey) {
        validerApikey(apiKey);

        henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(saksnummer);
        return ResponseEntity.noContent().build();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
