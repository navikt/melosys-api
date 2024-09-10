package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;

public interface FattVedtakInterface {
    void fattVedtak(Behandling behandling, FattVedtakRequest fattVedtakRequest);
}
