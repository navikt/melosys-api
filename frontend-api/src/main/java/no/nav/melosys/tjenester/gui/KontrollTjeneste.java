package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.kontroll.ferdigbehandling.FerdigbehandlingKontrollService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
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

    private final FerdigbehandlingKontrollService ferdigbehandlingKontrollService;
    private final Aksesskontroll aksesskontroll;

    public KontrollTjeneste(FerdigbehandlingKontrollService ferdigbehandlingKontrollService, Aksesskontroll aksesskontroll) {
        this.ferdigbehandlingKontrollService = ferdigbehandlingKontrollService;
        this.aksesskontroll = aksesskontroll;
    }

    @PostMapping("{behandlingID}/ferdigbehandling")
    public ResponseEntity<Void> kontrollerFerdigbehandling(
        @PathVariable("behandlingID") long behandlingID,
        @RequestParam(value = "skalRegisteropplysningerOppdateres", required = false) boolean skalRegisteropplysningerOppdateres,
        @RequestBody FattVedtakDto fattVedtakDto) throws ValideringException {

        if (fattVedtakDto.getVedtakstype() == null) {
            throw new FunksjonellException("Vedtakstype mangler.");
        }
        aksesskontroll.autoriser(behandlingID, skalRegisteropplysningerOppdateres ? Aksesstype.SKRIV : Aksesstype.LES);
        ferdigbehandlingKontrollService.kontroller(behandlingID, skalRegisteropplysningerOppdateres, fattVedtakDto.getBehandlingsresultatTypeKode());
        return ResponseEntity.noContent().build();
    }
}
