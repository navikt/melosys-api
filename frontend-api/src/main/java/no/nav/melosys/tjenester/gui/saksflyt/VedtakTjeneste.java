package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Aksesstype;
import no.nav.melosys.service.vedtak.*;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/saksflyt/vedtak")
@Api(tags = {"saksflyt", "vedtak"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VedtakTjeneste {
    private final VedtakServiceFasade vedtakServiceFasade;
    private final Aksesskontroll aksesskontroll;
    private final VedtakKontrollService vedtakKontrollService;

    @Autowired
    public VedtakTjeneste(VedtakServiceFasade vedtakServiceFasade, Aksesskontroll aksesskontroll, VedtakKontrollService vedtakKontrollService) {
        this.vedtakServiceFasade = vedtakServiceFasade;
        this.aksesskontroll = aksesskontroll;
        this.vedtakKontrollService = vedtakKontrollService;
    }

    @PostMapping("{behandlingID}/fatt")
    @ApiOperation(value = "Fatter et vedtak for en gitt behandling")
    public ResponseEntity<Void> fattVedtak(@PathVariable("behandlingID") long behandlingID,
                                           @RequestBody FattVedtakDto fattVedtakDto) throws ValideringException {
        if (fattVedtakDto == null || fattVedtakDto.getBehandlingsresultatTypeKode() == null || fattVedtakDto.getVedtakstype() == null) {
            throw new FunksjonellException("BehandlingsresultatTypeKode eller vedtakstype mangler.");
        }
        aksesskontroll.autoriserSkriv(behandlingID);

        vedtakServiceFasade.fattVedtak(behandlingID, lagFattVedtakRequest(fattVedtakDto, SubjectHandler.getInstance().getUserID()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{behandlingID}/endre")
    @ApiOperation(value = "Endrer et vedtak for en gitt behandling")
    public ResponseEntity<Void> endreVedtak(@PathVariable("behandlingID") long behandlingID,
                                            @RequestBody EndreVedtakDto endreVedtakDto) {
        if (endreVedtakDto.getBegrunnelseKode() == null) {
            throw new FunksjonellException("BegrunnelseKode mangler.");
        }
        aksesskontroll.autoriserSkriv(behandlingID);
        vedtakServiceFasade.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode(), endreVedtakDto.getFritekst(), endreVedtakDto.getFritekstSed());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("{behandlingID}/kontroller")
    @ApiOperation(value = "Gjør kontroll på vedtaket, og returnerer eventuelle feilmeldinger som liste med KontrollfeilDto")
    public ResponseEntity<Void> kontrollerVedtak(@PathVariable("behandlingID") long behandlingID,
                                 @RequestParam(value = "oppdaterRegisteropplysninger", required = false) boolean oppdaterRegisteropplysninger,
                                 @RequestBody FattVedtakDto fattVedtakDto) throws ValideringException {
        if (fattVedtakDto.getVedtakstype() == null) {
            throw new FunksjonellException("Vedtakstype mangler.");
        }
        aksesskontroll.autoriser(behandlingID, oppdaterRegisteropplysninger ? Aksesstype.SKRIV : Aksesstype.LES);
        vedtakKontrollService.kontrollerInnvilgelse(behandlingID, fattVedtakDto.getVedtakstype(), oppdaterRegisteropplysninger);
        return ResponseEntity.noContent().build();
    }

    private FattVedtakRequest lagFattVedtakRequest(FattVedtakDto fattVedtakDto, String bestillersId) {
        return new FattVedtakRequest.Builder()
            .medFritekst(fattVedtakDto.getFritekst())
            .medFritekstSed(fattVedtakDto.getFritekstSed())
            .medMottakerInstitusjoner(fattVedtakDto.getMottakerinstitusjoner())
            .medNyVurderingBakgrunn(fattVedtakDto.getNyVurderingBakgrunn())
            .medInnledningFritekst(fattVedtakDto.getInnledningFritekst())
            .medBegrunnelseFritekst(fattVedtakDto.getBegrunnelseFritekst())
            .medEktefelleFritekst(fattVedtakDto.getEktefelleFritekst())
            .medBarnFritekst(fattVedtakDto.getBarnFritekst())
            .medKopiMottakere(fattVedtakDto.getKopiMottakere())
            .medBehandlingsresultat(fattVedtakDto.getBehandlingsresultatTypeKode())
            .medVedtakstype(fattVedtakDto.getVedtakstype())
            .medBestillersId(bestillersId)
            .build();
    }
}
