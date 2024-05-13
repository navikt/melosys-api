package no.nav.melosys.tjenester.gui.kontroll;

import java.time.LocalDate;
import jakarta.validation.Valid;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.UnntaksperiodeKontrollService;
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
public class ValideringUnntaksperiodeTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ValideringUnntaksperiodeTjeneste.class);
    private final Aksesskontroll aksesskontroll;
    private final UnntaksperiodeKontrollService unntaksperiodeKontrollService;

    public ValideringUnntaksperiodeTjeneste(Aksesskontroll aksesskontroll, UnntaksperiodeKontrollService unntaksperiodeKontrollService) {
        this.aksesskontroll = aksesskontroll;
        this.unntaksperiodeKontrollService = unntaksperiodeKontrollService;
    }

    @PostMapping("/{behandlingID}/unntaksperiode")
    public ResponseEntity<Void> kanBehandlingGodkjennesMedPeriode(@PathVariable Long behandlingID,
                                                                  @RequestBody UnntaksperiodeRequestDto requestDto) {
        log.debug("/{}/unntaksperiode med data: {}", behandlingID, requestDto);
        aksesskontroll.autoriser(behandlingID, Aksesstype.LES);
        unntaksperiodeKontrollService.kontrollPeriode(behandlingID, requestDto.tilPeriode());
        return ResponseEntity.noContent().build();
    }

    @Valid
    record UnntaksperiodeRequestDto(
        @NotNull
        LocalDate periodeFom,

        @NotNull
        LocalDate periodeTom) {

        public ErPeriode tilPeriode() {
            return new Periode(periodeFom, periodeTom);
        }
    }
}
