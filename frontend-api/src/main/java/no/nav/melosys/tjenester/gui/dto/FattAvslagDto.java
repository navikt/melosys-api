package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.vedtak.FattAvslagRequest;

public record FattAvslagDto(String fritekst) {
     public FattAvslagRequest til() {
        return new FattAvslagRequest.Builder()
            .medFritekst(fritekst)
            .medBehandlingsresultat(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .build();
    }
}
