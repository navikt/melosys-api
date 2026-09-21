package no.nav.melosys.service.sak;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.aktoer.AktoerService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Protected
@RestController
@RequestMapping("/admin/fagsaker")
@Tags({
    @Tag(name = "fagsak"),
    @Tag(name = "admin")
})
public class FagsakAdminController {
    private final HenleggelseService henleggelseService;
    private final AktoerService aktoerService;
    private final FagsakService fagsakService;

    public FagsakAdminController(HenleggelseService henleggelseService,
                                 AktoerService aktoerService,
                                 FagsakService fagsakService) {
        this.henleggelseService = henleggelseService;
        this.aktoerService = aktoerService;
        this.fagsakService = fagsakService;
    }

    @GetMapping("/{saksnummer}/gsak-saksnummer")
    @Operation(summary = "Hent GSAK-saksnummer med Melosys-saksnummer",
        description = "Returnerer 404 hvis fagsaken ikke finnes eller mangler GSAK-saksnummer.")
    public ResponseEntity<GsakSaksnummerDto> hentGsakSaksnummer(@PathVariable String saksnummer) {
        Long gsakSaksnummer = fagsakService.hentFagsak(saksnummer).getGsakSaksnummer();
        if (gsakSaksnummer == null) {
            throw new IkkeFunnetException("Fagsak med saksnummer " + saksnummer + " mangler GSAK-saksnummer");
        }
        return ResponseEntity.ok(new GsakSaksnummerDto(gsakSaksnummer));
    }

    @PutMapping("/{behandlingID}/henlegg-bortfalt")
    public ResponseEntity<Void> henleggFagsakSomBortfalt(@PathVariable long behandlingID) {
        henleggelseService.henleggSakEllerBehandlingSomBortfalt(behandlingID);

        return ResponseEntity.noContent().build();
    }

    //Endre aktørID til en annen eksisterende aktørid.
    @PutMapping("/{saksnummer}/endreAktoerId/{aktoerid}")
    public ResponseEntity<Void> endreAktoerId(@PathVariable String saksnummer, @PathVariable String aktoerid) {
        aktoerService.endreAktørIdForBruker(saksnummer, aktoerid);

        return ResponseEntity.ok().build();
    }

    public record GsakSaksnummerDto(long gsakSaksnummer) {}

}
