package no.nav.melosys.service.vedtak

import no.nav.melosys.domain.Behandling

interface FattVedtakInterface {
    fun fattVedtak(behandling: Behandling?, fattVedtakRequest: FattVedtakRequest?)
}
