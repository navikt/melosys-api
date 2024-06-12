package no.nav.melosys.service.avgift.fakturering

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.BehandlingEndretStatusEvent
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.aktoer.AktoerHistorikkService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FaktureringEventListener(
    val behandlingService: BehandlingService,
    val behandlingsresultatService: BehandlingsresultatService,
    val aktoerHistorikkService: AktoerHistorikkService,
    val trygdeavgiftService: TrygdeavgiftService,
    val prosessinstansService: ProsessinstansService
) {
    @EventListener
    @Transactional
    fun oppdaterFakturaMottakerHvisNødvendig(event: BehandlingEndretStatusEvent) {
        if (event.behandlingsstatus != Behandlingsstatus.AVSLUTTET) {
            return
        }
        if (behandlingsresultatService.hentBehandlingsresultat(event.behandling.id).vedtakMetadata != null) {
            // Ved vedtak med fakturering opprettes fakturaserie med riktig mottaker allerede
            return
        }

        val behandling = behandlingService.hentBehandling(event.behandling.id)
        val fagsak = behandling.fagsak

        val fullmektigForTrygdeavgift = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        val gjeldendeFullmektigerNårBehandlingBleOpprettet =
            aktoerHistorikkService.hentHistoriskeAktørerPåTidspunkt(fagsak, Aktoersroller.FULLMEKTIG, behandling.registrertDato)
                .filter { it.fullmaktstyper.contains(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) }

        if (fullmektigForBetalingAvTrygdeavgiftBleEndret(
                fullmektigForTrygdeavgift, gjeldendeFullmektigerNårBehandlingBleOpprettet
            ) && trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(fagsak.saksnummer)
        ) {
            // Bestill prosess i stedet for å kalle faktureringskomponent direkte, for å få støtte for feilhåndtering og rekjøring
            prosessinstansService.opprettProsessinstansOppdaterFaktura(behandling)
        }
    }

    // Sjekk om fullmektig for betaling av trygdeavgift ble endret siden behandlingen ble opprettet.
    private fun fullmektigForBetalingAvTrygdeavgiftBleEndret(
        fullmektigForTrygdeavgift: Aktoer?, tidligereFullmektigerForTrygdeavgift: List<Aktoer>
    ): Boolean = fullmektigForTrygdeavgift != tidligereFullmektigerForTrygdeavgift.singleOrNull()
}
