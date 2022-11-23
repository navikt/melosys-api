package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

private val log = KotlinLogging.logger { }

@Service
class EndreSakService(
    private val lovligeKombinasjonerService: LovligeKombinasjonerService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val unleash: Unleash
) {
    @Transactional
    fun endre(
        saksnummer: String,
        nySakstype: Sakstyper,
        nySakstema: Sakstemaer,
        nyBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper,
        nyBehandlingsstatus: Behandlingsstatus?,
        nyBehandlingsfrist: LocalDate?
    ) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        validerSak(fagsak, nySakstype, nySakstema)
        val behandling = fagsak.hentAktivBehandling()
        validerBehandling(behandling)
        validerEndring(fagsak, nySakstype, nySakstema, nyBehandlingstema, nyBehandlingstype)

        if (fagsak.type == nySakstype && fagsak.tema == nySakstema) {
            behandlingService.endreBehandling(behandling.id, nyBehandlingstype, nyBehandlingstema, nyBehandlingsstatus, nyBehandlingsfrist)
            return
        }

        if (SaksbehandlingRegler.harTomFlyt(nySakstype, nySakstema, nyBehandlingstype, nyBehandlingstema, unleash.isEnabled("melosys.folketrygden.mvp"))) {
            mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id).ifPresent { mottatteOpplysningerService.slettOpplysninger(behandling.id) }
        } else {
            gjenopprettMottatteOpplysninger(nySakstype, behandling)
        }

        fagsakService.oppdaterSakstype(saksnummer, nySakstype)
        fagsakService.oppdaterSakstema(saksnummer, nySakstema)
        behandlingService.endreBehandling(behandling.id, nyBehandlingstype, nyBehandlingstema, nyBehandlingsstatus, nyBehandlingsfrist)

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)
        applicationEventPublisher.publishEvent(FagsakEndretAvSaksbehandler(fagsak.saksnummer))
        log.debug { "Ferdig med endring av sak $saksnummer (type: $nySakstype, tema: $nySakstema)" }
    }

    private fun validerBehandling(behandling: Behandling) {
        if (setOf(
                Behandlingsstatus.AVSLUTTET,
                Behandlingsstatus.IVERKSETTER_VEDTAK,
                Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
            ).contains(behandling.status)
        ) {
            throw FunksjonellException("Behandling ${behandling.id} med status ${behandling.status} kan ikke endres")
        }
    }

    private fun validerSak(fagsak: Fagsak, nySakstype: Sakstyper, nySakstema: Sakstemaer) {
        if ((fagsak.type != nySakstype || fagsak.tema != nySakstema) && !fagsak.kanEndreTypeOgTema()) {
            throw FunksjonellException("Sakstype eller tema kan ikke endres for ${fagsak.saksnummer}")
        }
    }

    private fun validerEndring(
        fagsak: Fagsak,
        nySakstype: Sakstyper,
        nySakstema: Sakstemaer,
        nyBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper
    ) {
        lovligeKombinasjonerService.validerBehandlingstemaOgBehandlingstypeForOpprettelse(
            fagsak.hovedpartRolle,
            nySakstype,
            nySakstema,
            nyBehandlingstema,
            nyBehandlingstype
        )
    }

    private fun gjenopprettMottatteOpplysninger(
        sakstype: Sakstyper, behandling: Behandling
    ) {
        if (sakstype == Sakstyper.EU_EOS && !unleash.isEnabled("melosys.tom_periode_og_land")) {
            validerPeriodeOgLand(behandling)
        }

        val mottatteOpplysninger = mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id).orElse(null)
        mottatteOpplysningerService.slettOpplysninger(behandling.id)
        mottatteOpplysningerService.opprettSøknad(
            behandling,
            mottatteOpplysninger?.mottatteOpplysningerData?.periode ?: Periode(),
            mottatteOpplysninger?.mottatteOpplysningerData?.soeknadsland ?: Soeknadsland()
        )
    }

    private fun validerPeriodeOgLand(behandling: Behandling) {
        if (!behandling.harPeriodeOgLand()) {
            throw FunksjonellException("Du må legge inn periode og land i flyten for å kunne bytte til sakstype EU/EØS")
        }
    }
}
