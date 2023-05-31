package no.nav.melosys.tjenester.gui.kontroll;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.melosys.tjenester.gui.dto.kontroller.FerdigbehandlingKontrollerDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/kontroll")
@Api(tags = "kontroll")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class KontrollTjeneste {

    private final FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;
    private final Aksesskontroll aksesskontroll;
    private final EessiService eessiService;
    private final BehandlingService behandlingService;

    public KontrollTjeneste(FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade, Aksesskontroll aksesskontroll,
                            EessiService eessiService, BehandlingService behandlingService) {
        this.ferdigbehandlingKontrollFacade = ferdigbehandlingKontrollFacade;
        this.aksesskontroll = aksesskontroll;
        this.eessiService = eessiService;
        this.behandlingService = behandlingService;
    }

    /**
     * @deprecated
     * "Erstattes av erBucLukket endepunktet. Fjernes etter fiks på prodfeil er ute"
     */
    @GetMapping("{behandlingId}/sed/{sedType}")
    public ResponseEntity<Boolean> kanOppretteSedTypePaaBuc(@PathVariable("behandlingId") Long behandlingId,
                                                            @PathVariable("sedType") SedType sedType) {
        var behandling = behandlingService.hentBehandling(behandlingId);
        var rinaSaksnummer = eessiService.finnSakForGsakSaksnummer(behandling.getFagsak().getGsakSaksnummer());
        if (rinaSaksnummer.isEmpty()) {
            throw new FunksjonellException("Finner ikke rinaSaksnummer for behandling %d".formatted(behandlingId));
        }
        return ResponseEntity.ok(eessiService.kanOppretteSedTyperPåBuc(rinaSaksnummer.get(), sedType));
    }

    @GetMapping("{behandlingId}/buc")
    public ResponseEntity<Boolean> erBucLukket(@PathVariable("behandlingId") Long behandlingId) {
        var behandling = behandlingService.hentBehandling(behandlingId);
        return ResponseEntity.ok(eessiService.erBucLukket(behandling.getFagsak().getGsakSaksnummer()));
    }

    @PostMapping("/ferdigbehandling")
    public ResponseEntity<Void> kontrollerFerdigbehandling(@RequestBody FerdigbehandlingKontrollerDto ferdigbehandlingKontrollerDto) throws ValideringException {

        if (ferdigbehandlingKontrollerDto.vedtakstype() == null) {
            throw new FunksjonellException("Vedtakstype mangler.");
        }
        aksesskontroll.autoriser(
            ferdigbehandlingKontrollerDto.behandlingID(),
            ferdigbehandlingKontrollerDto.skalRegisteropplysningerOppdateres() ? Aksesstype.SKRIV : Aksesstype.LES
        );

        ferdigbehandlingKontrollFacade.kontroller(
            ferdigbehandlingKontrollerDto.behandlingID(),
            ferdigbehandlingKontrollerDto.skalRegisteropplysningerOppdateres(),
            ferdigbehandlingKontrollerDto.behandlingsresultattype(),
            ferdigbehandlingKontrollerDto.kontrollerSomSkalIgnoreres()
        );

        return ResponseEntity.noContent().build();
    }
}
