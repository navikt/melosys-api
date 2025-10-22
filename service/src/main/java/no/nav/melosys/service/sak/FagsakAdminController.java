package no.nav.melosys.service.sak;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@RequestMapping("/admin/fagsaker")
@Tags({
    @Tag(name = "fagsak"),
    @Tag(name = "admin")
})
public class FagsakAdminController {
    private final HenleggelseService henleggelseService;
    private final FagsakRepository fagsakRepository;
    private final AktoerService aktoerService;

    public FagsakAdminController(HenleggelseService henleggelseService,
                                FagsakRepository fagsakRepository,
                                AktoerService aktoerService) {
        this.henleggelseService = henleggelseService;
        this.fagsakRepository = fagsakRepository;
        this.aktoerService = aktoerService;
    }

    @PutMapping("/{behandlingID}/henlegg-bortfalt")
    public ResponseEntity<Void> henleggFagsakSomBortfalt(@PathVariable long behandlingID) {
        henleggelseService.henleggSakEllerBehandlingSomBortfalt(behandlingID);

        return ResponseEntity.noContent().build();
    }

    //Endre aktørID til en annen eksisterende aktørid.
    @PutMapping("/{saksnummer}/endreAktoerId/{aktoerid}")
    public ResponseEntity<Void> endreAktoerId(@PathVariable String saksnummer, @PathVariable String aktoerid) {
        if (aktoerid == null || aktoerid.trim().isEmpty()) {
            throw new IllegalArgumentException("Aktør ID cannot be null or empty");
        }

        Fagsak fagsak = fagsakRepository.findById(saksnummer)
                .orElseThrow(() -> new IllegalArgumentException("Fagsak not found with saksnummer: " + saksnummer));

        aktoerService.endreAktørId(fagsak, aktoerid);

        return ResponseEntity.noContent().build();
    }

}
