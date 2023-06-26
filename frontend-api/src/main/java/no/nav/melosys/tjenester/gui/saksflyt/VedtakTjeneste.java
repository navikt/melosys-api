package no.nav.melosys.tjenester.gui.saksflyt;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.vedtak.FattVedtakRequest;
import no.nav.melosys.service.vedtak.VedtaksfattingFasade;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import no.nav.security.token.support.core.api.Protected;
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
    private final VedtaksfattingFasade vedtaksfattingFasade;
    private final Aksesskontroll aksesskontroll;

    public VedtakTjeneste(VedtaksfattingFasade vedtaksfattingFasade, Aksesskontroll aksesskontroll) {
        this.vedtaksfattingFasade = vedtaksfattingFasade;
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

        vedtaksfattingFasade.fattVedtak(behandlingID, lagFattVedtakRequest(fattVedtakDto, SubjectHandler.getInstance().getUserID()));
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
        vedtaksfattingFasade.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode(), endreVedtakDto.getFritekst(), endreVedtakDto.getFritekstSed());
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
            .medBetalingsIntervall(fattVedtakDto.getBetalingsintervall())
            .medKopiTilArbeidsgiver(fattVedtakDto.getKopiTilArbeidsgiver())
            .build();
    }
}
