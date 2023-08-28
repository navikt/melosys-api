package no.nav.melosys.tjenester.gui.kontroll;

import java.util.Collection;
import java.util.List;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.kontroll.feature.postadresse.PostadresseKontrollService;
import no.nav.melosys.service.kontroll.feature.postadresse.PostadressesjekkKontekst;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.melosys.service.validering.Kontrollfeil;
import no.nav.melosys.tjenester.gui.dto.kontroller.FerdigbehandlingKontrollerDto;
import no.nav.melosys.tjenester.gui.dto.kontroller.KontrollerAdresseBrukerFullmektigDto;
import no.nav.melosys.tjenester.gui.dto.kontroller.KontrollerAdresseBrukerFullmektigResponse;
import no.nav.melosys.tjenester.gui.dto.kontroller.KontrollerFerdigbehandlingResponse;
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
    private final Aksesskontroll aksesskontroll;
    private final FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;
    private final BehandlingService behandlingService;
    private final EessiService eessiService;
    private final PostadresseKontrollService postadresseKontrollService;

    public KontrollTjeneste(FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade, Aksesskontroll aksesskontroll,
                            EessiService eessiService, BehandlingService behandlingService, PostadresseKontrollService postadresseKontrollService) {
        this.aksesskontroll = aksesskontroll;
        this.ferdigbehandlingKontrollFacade = ferdigbehandlingKontrollFacade;
        this.behandlingService = behandlingService;
        this.eessiService = eessiService;
        this.postadresseKontrollService = postadresseKontrollService;
    }

    @GetMapping("{behandlingId}/erBucAapen")
    public ResponseEntity<Boolean> erBucAapen(@PathVariable("behandlingId") Long behandlingId) {
        var behandling = behandlingService.hentBehandling(behandlingId);
        return ResponseEntity.ok(eessiService.erBucAapen(behandling.getFagsak().getGsakSaksnummer()));
    }

    @PostMapping("/adresse")
    public ResponseEntity<KontrollerAdresseBrukerFullmektigResponse> harRegistrertAdresse(@RequestBody KontrollerAdresseBrukerFullmektigDto kontrollDto) {
        var kontekst = new PostadressesjekkKontekst(kontrollDto.getBehandlingID(), kontrollDto.getBrukerID(), kontrollDto.getOrgnr());
        List<Kontrollfeil> kontrollfeilList = postadresseKontrollService.kontroller(kontekst);
        return ResponseEntity.ok(new KontrollerAdresseBrukerFullmektigResponse(kontrollfeilList.stream().map(Kontrollfeil::tilDto).toList()));
    }

    @PostMapping("/ferdigbehandling")
    public ResponseEntity<KontrollerFerdigbehandlingResponse> kontrollerFerdigbehandling(@RequestBody FerdigbehandlingKontrollerDto ferdigbehandlingKontrollerDto) {

        if (ferdigbehandlingKontrollerDto.vedtakstype() == null) {
            throw new FunksjonellException("Vedtakstype mangler.");
        }
        aksesskontroll.autoriser(
            ferdigbehandlingKontrollerDto.behandlingID(),
            ferdigbehandlingKontrollerDto.skalRegisteropplysningerOppdateres() ? Aksesstype.SKRIV : Aksesstype.LES
        );

        Collection<Kontrollfeil> kontrollfeil = ferdigbehandlingKontrollFacade.kontroller(
            ferdigbehandlingKontrollerDto.behandlingID(),
            ferdigbehandlingKontrollerDto.skalRegisteropplysningerOppdateres(),
            ferdigbehandlingKontrollerDto.behandlingsresultattype(),
            ferdigbehandlingKontrollerDto.kontrollerSomSkalIgnoreres()
        );

        return ResponseEntity.ok(new KontrollerFerdigbehandlingResponse(kontrollfeil.stream().map(Kontrollfeil::tilDto).toList()));
    }
}
