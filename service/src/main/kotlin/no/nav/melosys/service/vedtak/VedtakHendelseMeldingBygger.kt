package no.nav.melosys.service.vedtak

import no.nav.melosys.domain.Behandling
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.Periode
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component

@Component
class VedtakHendelseMeldingBygger(
    private val behandlingsresultatService: BehandlingsresultatService
) {
    fun bygg(behandling: Behandling, folkeregisterIdent: String): MelosysHendelse {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val fagsak = behandling.fagsak

        return MelosysHendelse(
            melding = VedtakHendelseMelding(
                folkeregisterIdent = folkeregisterIdent,
                sakstype = fagsak.type,
                sakstema = fagsak.tema,
                behandligsresultatType = behandlingsresultat.hentType(),
                vedtakstype = behandlingsresultat.vedtakMetadata?.vedtakstype,
                medlemskapsperioder = behandlingsresultat.medlemskapsperioder.map {
                    Periode(it.hentFom(), it.hentTom(), it.hentInnvilgelsesresultat())
                },
                lovvalgsperioder = behandlingsresultat.lovvalgsperioder.map {
                    Periode(it.hentFom(), it.hentTom(), it.hentInnvilgelsesresultat())
                }
            )
        )
    }
}
