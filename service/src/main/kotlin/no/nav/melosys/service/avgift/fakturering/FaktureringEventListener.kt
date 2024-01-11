package no.nav.melosys.service.avgift.fakturering

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.BehandlingEndretStatusEvent
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.aktoer.AktoerHistorikkService
import no.nav.melosys.service.behandling.BehandlingService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FaktureringEventListener(
    val behandlingService: BehandlingService,
    val aktoerHistorikkService: AktoerHistorikkService,
    val prosessinstansService: ProsessinstansService
) {
    @EventListener
    @Transactional
    fun oppdaterFakturaMottakerHvisNødvendig(event: BehandlingEndretStatusEvent) {
        if (event.behandlingsstatus != Behandlingsstatus.AVSLUTTET) {
            return
        }

        val behandling = behandlingService.hentBehandling(event.behandling.id)
        val fagsak = behandling.fagsak
        val fullmektigForTrygdeavgift = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT).orElse(null)
        // TODO Trenger vi sjekk om saken gjelder trygdeavgift i tillegg? (ev. fakturaserieReferanse på behandlingsresultater)
        if (fullmektigForTrygdeavgift == null) {
            return
        }

        val gyldigeFullmektigerNårBehandlingBleOpprettet =
            aktoerHistorikkService.hentGyldigeAktørerPåTidspunkt(fagsak, Aktoersroller.FULLMEKTIG, behandling.registrertDato)
                .filter { it.fullmaktstyper.contains(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) }

        if (fakturaMottakerMåOppdateres(fullmektigForTrygdeavgift, gyldigeFullmektigerNårBehandlingBleOpprettet)) {
            // Bestill prosess i stedet for å kalle faktureringskomponent for feilhåndtering og rekjøring
            prosessinstansService.opprettProsessinstansOppdaterFaktura(fagsak.saksnummer)
        }
    }

    // Sjekk om fullmektig for betaling av trygdeavgift ble endret siden behandlingen ble opprettet.
    private fun fakturaMottakerMåOppdateres(
        fullmektigForTrygdeavgift: Aktoer, tidligereFullmektigerForTrygdeavgift: List<Aktoer>
    ): Boolean {
        if (tidligereFullmektigerForTrygdeavgift.size != 1) {
            return true
        }
        val tidligereFullmektigForTrygdeavgift = tidligereFullmektigerForTrygdeavgift.single()
        return fullmektigForTrygdeavgift != tidligereFullmektigForTrygdeavgift
    }
}
