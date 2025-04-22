package no.nav.melosys.service.sak;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Unprotected
@RestController
@RequestMapping("/admin/fagsaker")
@Tags({
    @Tag(name = "fagsak"),
    @Tag(name = "admin")
})
public class FagsakAdminController {
    private final HenleggelseService henleggelseService;

    public FagsakAdminController(HenleggelseService henleggelseService) {
        this.henleggelseService = henleggelseService;
    }

    @PutMapping("/{behandlingID}/henlegg-bortfalt")
    public ResponseEntity<Void> henleggFagsakSomBortfalt(@PathVariable long behandlingID) {
        henleggelseService.henleggSakEllerBehandlingSomBortfalt(behandlingID);

        return ResponseEntity.noContent().build();
    }
}
