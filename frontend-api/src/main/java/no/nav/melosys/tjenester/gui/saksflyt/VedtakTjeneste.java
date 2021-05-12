package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.vedtak.FattEosVedtakRequest;
import no.nav.melosys.service.vedtak.FattFtrlVedtakRequest;
import no.nav.melosys.service.vedtak.FattVedtakRequest;
import no.nav.melosys.service.vedtak.VedtakServiceFasade;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattEosVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattFtrlVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
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
    private final TilgangService tilgangService;

    @Autowired
    public VedtakTjeneste(VedtakServiceFasade vedtakServiceFasade, TilgangService tilgangService) {
        this.vedtakServiceFasade = vedtakServiceFasade;
        this.tilgangService = tilgangService;
    }

    @PostMapping("{behandlingID}/fatt")
    @ApiOperation(value = "Fatter et vedtak for en gitt behandling")
    public ResponseEntity<Void> fattVedtak(@PathVariable("behandlingID") long behandlingID,
                                           @RequestBody FattVedtakDto fattVedtakDto) throws ValideringException {
        if (fattVedtakDto == null || fattVedtakDto.getBehandlingsresultatTypeKode() == null || fattVedtakDto.getVedtakstype() == null) {
            throw new FunksjonellException("BehandlingsresultatTypeKode eller vedtakstype mangler.");
        }
        tilgangService.sjekkTilgang(behandlingID);

        vedtakServiceFasade.fattVedtak(behandlingID, lagFattVedtakRequest(fattVedtakDto));
        return ResponseEntity.ok().build();
    }

    @PostMapping("{behandlingID}/endre")
    @ApiOperation(value = "Endrer et vedtak for en gitt behandling")
    public ResponseEntity<Void> endreVedtak(@PathVariable("behandlingID") long behandlingID,
                                            @RequestBody EndreVedtakDto endreVedtakDto)
        throws FunksjonellException, TekniskException {
        if (endreVedtakDto.getBegrunnelseKode() == null) {
            throw new FunksjonellException("BegrunnelseKode mangler.");
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakServiceFasade.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode(), endreVedtakDto.getFritekst(), endreVedtakDto.getFritekstSed());
        return ResponseEntity.ok().build();
    }

    private FattVedtakRequest lagFattVedtakRequest(FattVedtakDto fattVedtakDto) throws FunksjonellException {
        FattVedtakRequest.Builder<?> fattVedtakRequest;

        if (fattVedtakDto instanceof FattEosVedtakDto eosVedtakDto) {
            fattVedtakRequest = new FattEosVedtakRequest.Builder()
                .medFritekst(eosVedtakDto.getFritekst())
                .medFritekstSed(eosVedtakDto.getFritekstSed())
                .medMottakerInstitusjoner(eosVedtakDto.getMottakerinstitusjoner())
                .medRevurderBegrunnelse(eosVedtakDto.getRevurderBegrunnelse());
        } else if (fattVedtakDto instanceof FattFtrlVedtakDto ftrlVedtakDto) {
            fattVedtakRequest = new FattFtrlVedtakRequest.Builder()
                .medFritekstBegrunnelse(ftrlVedtakDto.getFritekstBegrunnelse());
        } else {
            throw new FunksjonellException("Vedtakstype er ikke støttet");
        }

        fattVedtakRequest
            .medBehandlingsresultat(fattVedtakDto.getBehandlingsresultatTypeKode())
            .medVedtakstype(fattVedtakDto.getVedtakstype());

        return fattVedtakRequest.build();
    }
}
