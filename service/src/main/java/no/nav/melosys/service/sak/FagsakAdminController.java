package no.nav.melosys.service.sak;

import no.nav.melosys.service.AdminController;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Unprotected
@RestController
@RequestMapping("/admin/fagsaker")
public class FagsakAdminController implements AdminController {
    private final String apiKey;
    private final HenleggelseService henleggelseService;

    private static final Logger log = LoggerFactory.getLogger(FagsakAdminController.class);

    public FagsakAdminController(@Value("${Melosys-admin.apikey}") String apiKey, HenleggelseService henleggelseService) {
        this.apiKey = apiKey;
        this.henleggelseService = henleggelseService;
    }

    @PutMapping("/{behandlingID}/henlegg-bortfalt")
    public ResponseEntity<Void> henleggFagsakSomBortfalt(@PathVariable long behandlingID,
                                                         @RequestHeader(API_KEY_HEADER) String apiKey) {
        validerApikey(apiKey);

        henleggelseService.henleggSakEllerBehandlingSomBortfalt(behandlingID);

        return ResponseEntity.noContent().build();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
