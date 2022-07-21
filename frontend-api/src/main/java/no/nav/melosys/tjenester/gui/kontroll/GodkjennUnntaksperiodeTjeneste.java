package no.nav.melosys.tjenester.gui.kontroll;

import java.time.LocalDate;
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.Api;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.GodkjennUnntakKontrollService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.security.token.support.core.api.Protected;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/kontroll")
@Api(tags = "kontroll")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class GodkjennUnntaksperiodeTjeneste {

    private static final Logger log = LoggerFactory.getLogger(GodkjennUnntaksperiodeTjeneste.class);
    private final Aksesskontroll aksesskontroll;
    private final GodkjennUnntakKontrollService godkjennUnntakKontrollService;

    public GodkjennUnntaksperiodeTjeneste(Aksesskontroll aksesskontroll, GodkjennUnntakKontrollService godkjennUnntakKontrollService) {
        this.aksesskontroll = aksesskontroll;
        this.godkjennUnntakKontrollService = godkjennUnntakKontrollService;
    }

    @PostMapping("/{behandlingID}/godkjenn-unntaksperiode")
    public ResponseEntity<Void> kanBehandlingGodkjennes(@PathVariable Long behandlingID) {
        log.debug("/godkjenn-unntaksperiode med behandlingID {}", behandlingID);
        aksesskontroll.autoriser(behandlingID, Aksesstype.LES);
        godkjennUnntakKontrollService.utførKontroll(behandlingID);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{behandlingID}/kontroll-periode")
    public ResponseEntity<Void> kanBehandlingGodkjennesMedPeriode(@PathVariable Long behandlingID,
                                                                  @RequestBody GodkjennUnntaksperiodeManueltRequestDto requestDto) {
        log.debug("/{}/kontroll-periode med data: {}", behandlingID, requestDto);
        aksesskontroll.autoriser(behandlingID, Aksesstype.LES);
        godkjennUnntakKontrollService.kontrollPeriode(behandlingID, requestDto.periodeFom(), requestDto.periodeTom());
        return ResponseEntity.noContent().build();
    }

    @Valid
    record GodkjennUnntaksperiodeManueltRequestDto(
        @NotNull
        @JsonFormat(pattern = "dd.MM.yyyy")
        LocalDate periodeFom,

        @NotNull
        @JsonFormat(pattern = "dd.MM.yyyy")
        LocalDate periodeTom) {
    }
}
