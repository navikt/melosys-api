package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.tilgang.Aksesskontroll;
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

    @Autowired
    public VedtakTjeneste(VedtakServiceFasade vedtakServiceFasade, Aksesskontroll aksesskontroll) {
        this.vedtakServiceFasade = vedtakServiceFasade;
        this.aksesskontroll = aksesskontroll;
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
        return ResponseEntity.ok().build();
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
        return ResponseEntity.ok().build();
    }

    private FattVedtakRequest lagFattVedtakRequest(FattVedtakDto fattVedtakDto, String bestillersId) {
        FattVedtakRequest.Builder<?> fattVedtakRequest;

        if (fattVedtakDto instanceof FattEosVedtakDto eosVedtakDto) {
            fattVedtakRequest = new FattEosVedtakRequest.Builder()
                .medFritekst(eosVedtakDto.getFritekst())
                .medFritekstSed(eosVedtakDto.getFritekstSed())
                .medMottakerInstitusjoner(eosVedtakDto.getMottakerinstitusjoner())
                .medRevurderBegrunnelse(eosVedtakDto.getRevurderBegrunnelse());
        } else if (fattVedtakDto instanceof FattFtrlVedtakDto ftrlVedtakDto) {
            fattVedtakRequest = new FattFtrlVedtakRequest.Builder()
                .medFritekstInnledning(ftrlVedtakDto.getFritekstInnledning())
                .medFritekstBegrunnelse(ftrlVedtakDto.getFritekstBegrunnelse())
                .medFritekstEktefelle(ftrlVedtakDto.getFritekstEktefelle())
                .medFritekstBarn(ftrlVedtakDto.getFritekstBarn())
                .medKopiMottakere(ftrlVedtakDto.getKopiMottakere())
                .medBestillersId(bestillersId);
        } else if (fattVedtakDto instanceof FattTrygdeavtaleVedtakDto ftrlVedtakDto) {
            fattVedtakRequest = new FattTrygdeavtaleVedtakRequest.Builder()
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
