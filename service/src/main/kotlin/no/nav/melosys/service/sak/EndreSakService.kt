package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingEndretAvSaksbehandlerEvent
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakEndretAvSaksbehandler
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Sakstyper.*
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.transaction.Transactional

private val log = KotlinLogging.logger { }

@Service
class EndreSakService(
    private val lovligeKombinasjonerService: LovligeKombinasjonerService,
    private val fagsakService: FagsakService,
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
        nyBehandlingsstatus: Behandlingsstatus,
        nyBehandlingsfrist: LocalDate?
    ) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        validerSak(fagsak, nySakstype, nySakstema)
        val behandling = fagsak.hentAktivBehandling()
        validerBehandling(behandling)
        validerEndring(fagsak, nySakstype, nySakstema, nyBehandlingstema, nyBehandlingstype)

        val events = utledEvents(fagsak, behandling, nySakstype, nySakstema, nyBehandlingstema, nyBehandlingstype, nyBehandlingsfrist)
        fagsakService.oppdaterFagsakOgBehandling(
            saksnummer,
            nySakstype,
            nySakstema,
            nyBehandlingstema,
            nyBehandlingstype,
            nyBehandlingsstatus,
            nyBehandlingsfrist
        )

        if (sakEndres(fagsak, nySakstype, nySakstema) || behandlingEndreTypeTema(behandling, nyBehandlingstema, nyBehandlingstype)) {
            if (SaksbehandlingRegler.harTomFlyt(nySakstype, nySakstema, nyBehandlingstype, nyBehandlingstema, unleash.isEnabled("melosys.folketrygden.mvp"))) {
                mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id).ifPresent { mottatteOpplysningerService.slettOpplysninger(behandling.id) }
            } else {
                gjenopprettMottatteOpplysninger(nySakstype, behandling)
            }

            oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)
        }

        events.forEach { event ->
            applicationEventPublisher.publishEvent(event)
        }
        log.debug { "Ferdig med endring av sak $saksnummer (type: $nySakstype, tema: $nySakstema)" }
    }

    private fun validerSak(fagsak: Fagsak, nySakstype: Sakstyper, nySakstema: Sakstemaer) {
        if (sakEndres(fagsak, nySakstype, nySakstema) && !fagsak.kanEndreTypeOgTema()) {
            throw FunksjonellException("Sakstype eller tema kan ikke endres for ${fagsak.saksnummer}")
        }
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

    private fun validerEndring(
        fagsak: Fagsak,
        nySakstype: Sakstyper,
        nySakstema: Sakstemaer,
        nyBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper
    ) {
        lovligeKombinasjonerService.validerOpprettelseOgEndring(
            fagsak.hovedpartRolle,
            nySakstype,
            nySakstema,
            nyBehandlingstema,
            nyBehandlingstype
        )
    }

    private fun utledEvents(
        fagsak: Fagsak,
        behandling: Behandling,
        nySakstype: Sakstyper,
        nySakstema: Sakstemaer,
        nyBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper,
        nyBehandlingsfrist: LocalDate?
    ): List<ApplicationEvent> {
        val applicationEvents = mutableListOf<ApplicationEvent>()

        if (sakEndres(fagsak, nySakstype, nySakstema)) {
            applicationEvents.add(FagsakEndretAvSaksbehandler(fagsak.saksnummer))
        }

        if (behandlingEndreTypeTema(behandling, nyBehandlingstema, nyBehandlingstype) || nyBehandlingsfrist != null) {
            applicationEvents.add(BehandlingEndretAvSaksbehandlerEvent(behandling.id, behandling))
        }
        return applicationEvents
    }

    private fun sakEndres(
        fagsak: Fagsak,
        nySakstype: Sakstyper,
        nySakstema: Sakstemaer
    ) = fagsak.type != nySakstype || fagsak.tema != nySakstema

    private fun behandlingEndreTypeTema(
        behandling: Behandling,
        nyBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper
    ) = behandling.tema != nyBehandlingstema || behandling.type != nyBehandlingstype

    private fun gjenopprettMottatteOpplysninger(
        nySakstype: Sakstyper, behandling: Behandling
    ) {
        if (nySakstype == EU_EOS && !unleash.isEnabled("melosys.tom_periode_og_land")) {
            validerPeriodeOgLand(behandling)
        }

        val mottatteOpplysninger = mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id).orElse(null)
        mottatteOpplysningerService.slettOpplysninger(behandling.id)
        mottatteOpplysningerService.opprettSøknad(
            behandling,
            mottatteOpplysninger?.mottatteOpplysningerData?.periode ?: Periode(),
            søknadslandTilGjenoppretting(nySakstype, mottatteOpplysninger?.mottatteOpplysningerData?.soeknadsland)
        )
    }

    private fun validerPeriodeOgLand(behandling: Behandling) {
        if (!behandling.harPeriodeOgLand()) {
            throw FunksjonellException("Du må legge inn periode og land i flyten for å kunne bytte til sakstype EU/EØS")
        }
        val landkoder = behandling.mottatteOpplysninger?.mottatteOpplysningerData?.soeknadsland?.landkoder!!
        if (!landkoder.all { land -> landkodeErGyldigForSakstype(land, EU_EOS) }) {
            throw FunksjonellException("Du må legge til støttet EU/EØS-land for å kunne bytte til sakstype EU/EØS")
        }
    }

    private fun søknadslandTilGjenoppretting(nySakstype: Sakstyper, soeknadsland: Soeknadsland?): Soeknadsland {
        val tomSøknadsland = Soeknadsland()

        if (soeknadsland == null || soeknadsland.landkoder.isEmpty()) {
            return tomSøknadsland
        }
        if ((nySakstype != EU_EOS) && (soeknadsland.erUkjenteEllerAlleEosLand || (soeknadsland.landkoder.size != 1))) {
            return tomSøknadsland
        }

        return if (landkodeErGyldigForSakstype(soeknadsland.landkoder[0], nySakstype)) soeknadsland else tomSøknadsland
    }

    private fun landkodeErGyldigForSakstype(land: String, sakstype: Sakstyper): Boolean =
        when (sakstype) {
            EU_EOS -> Landkoder.values().any { landkoder -> landkoder.kode == land }
            TRYGDEAVTALE -> Trygdeavtale_myndighetsland.values().any { landkoder -> landkoder.kode == land }
            FTRL -> true
        }
}
