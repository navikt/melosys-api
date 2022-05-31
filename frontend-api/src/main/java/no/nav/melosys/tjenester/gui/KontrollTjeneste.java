package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.melosys.tjenester.gui.dto.kontroller.FerdigbehandlingKontrollerDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    @PostMapping("ferdigbehandling")
    public ResponseEntity<Void> kontrollerFerdigbehandling(@RequestBody FerdigbehandlingKontrollerDto ferdigbehandlingKontrollerDto) throws ValideringException {

        if (ferdigbehandlingKontrollerDto.vedtakstype() == null) {
            throw new FunksjonellException("Vedtakstype mangler.");
        }
        aksesskontroll.autoriser(ferdigbehandlingKontrollerDto.behandlingID(), ferdigbehandlingKontrollerDto.skalRegisteropplysningerOppdateres() ? Aksesstype.SKRIV : Aksesstype.LES);
        ferdigbehandlingKontrollService.kontroller(ferdigbehandlingKontrollerDto.behandlingID(), ferdigbehandlingKontrollerDto.skalRegisteropplysningerOppdateres(), ferdigbehandlingKontrollerDto.behandlingsresultattype());
        return ResponseEntity.noContent().build();
    }
}
