package no.nav.melosys.service.sak

import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.VedtakMetadataLagretEvent
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
class EndreSakService(
    private val fagsakService: FagsakService,
    private val behandlingsgrunnlagService: BehandlingsgrunnlagService,
    private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun endre(saksnummer: String, sakstype: Sakstyper, sakstema: Sakstemaer) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        valider(fagsak, sakstype, sakstema)

        // TODO søknad -> fra behandlingsgrunnlagService
        fagsakService.oppdaterSakstype(saksnummer, sakstype)
        fagsakService.oppdaterSakstema(saksnummer, sakstema)
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(fagsak.hentAktivBehandling().id, false)

        // TODO oppdater oppgave
        applicationEventPublisher.publishEvent(VedtakMetadataLagretEvent(1L))
    }

    private fun valider(
        fagsak: Fagsak, sakstype: Sakstyper, sakstema: Sakstemaer
    ) {
        if (fagsak.type == sakstype && fagsak.tema == sakstema) {
            throw FunksjonellException("Sak ${fagsak.saksnummer} har allerede type ${fagsak.type} og tema ${fagsak.tema}")
        }
        if (!fagsak.kanEndres()) {
            throw FunksjonellException("Sak ${fagsak.saksnummer} kan ikke endres")
        }
    }
}
