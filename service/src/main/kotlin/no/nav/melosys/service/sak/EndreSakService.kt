package no.nav.melosys.service.sak

import mu.KotlinLogging
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
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
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
    private val lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val saksbehandlingRegler: SaksbehandlingRegler
) {
    @Transactional
    fun endre(
        saksnummer: String,
        nySakstype: Sakstyper,
        nySakstema: Sakstemaer,
        nyBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper,
        nyBehandlingsstatus: Behandlingsstatus,
        nyMottaksdato: LocalDate?
    ) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        validerSak(fagsak, nySakstype, nySakstema)
        val behandling = fagsak.hentAktivBehandling()
        validerBehandling(behandling)

        val sakEndres = sakEndres(fagsak, nySakstype, nySakstema)
        val behandlingTemaEllerTypeEndres = behandlingTemaTypeEndres(behandling, nyBehandlingstema, nyBehandlingstype)
        if (sakEndres || behandlingTemaEllerTypeEndres) {
            validerEndring(
                fagsak,
                behandling,
                nySakstype,
                nySakstema,
                nyBehandlingstema,
                nyBehandlingstype,
            )
        }

        fagsakService.oppdaterFagsakOgBehandling(
            saksnummer,
            nySakstype,
            nySakstema,
            nyBehandlingstema,
            nyBehandlingstype,
            nyBehandlingsstatus,
            nyMottaksdato
        )

        if (sakEndres || behandlingTemaEllerTypeEndres) {
            if (saksbehandlingRegler.harIngenFlyt(
                    nySakstype,
                    nySakstema,
                    nyBehandlingstype,
                    nyBehandlingstema
                )
            ) {
                mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id)
                    .ifPresent { mottatteOpplysningerService.slettOpplysninger(behandling.id) }
            } else {
                gjenopprettMottatteOpplysninger(nySakstype, behandling)
            }

            if (behandling.sisteOpplysningerHentetDato != null) {
                oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)
            }
        }

        if (sakEndres) {
            applicationEventPublisher.publishEvent(FagsakEndretAvSaksbehandler(fagsak.saksnummer))
        } else if (behandlingTemaEllerTypeEndres || nyMottaksdato != null) {
            applicationEventPublisher.publishEvent(BehandlingEndretAvSaksbehandlerEvent(behandling.id, behandling))
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
        behandling: Behandling,
        nySakstype: Sakstyper,
        nySakstema: Sakstemaer,
        nyBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper,
    ) {
        val behandlingsresultatMedAnmodningsperioder =
            behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(behandling.id)
        if (behandlingsresultatMedAnmodningsperioder.erArtikkel16MedSendtAnmodningOmUnntak()) {
            throw FunksjonellException("Behandling ${behandling.id} har sendt anmodning om unntak og kan ikke lenger endres")
        }

        lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(
            behandling,
            fagsak.hovedpartRolle,
            nySakstype,
            nySakstema,
            nyBehandlingstema,
            nyBehandlingstype
        )
    }

    private fun sakEndres(
        fagsak: Fagsak,
        nySakstype: Sakstyper,
        nySakstema: Sakstemaer
    ) = fagsak.type != nySakstype || fagsak.tema != nySakstema

    private fun behandlingTemaTypeEndres(
        behandling: Behandling,
        nyBehandlingstema: Behandlingstema,
        nyBehandlingstype: Behandlingstyper
    ) = behandling.tema != nyBehandlingstema || behandling.type != nyBehandlingstype

    private fun gjenopprettMottatteOpplysninger(
        nySakstype: Sakstyper, behandling: Behandling
    ) {
        val mottatteOpplysninger = mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id).orElse(null)
        mottatteOpplysningerService.slettOpplysninger(behandling.id)
        mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(
            behandling,
            mottatteOpplysninger?.mottatteOpplysningerData?.periode ?: Periode(),
            søknadslandTilGjenoppretting(nySakstype, mottatteOpplysninger?.mottatteOpplysningerData?.soeknadsland)
        )
    }

    private fun søknadslandTilGjenoppretting(nySakstype: Sakstyper, soeknadsland: Soeknadsland?): Soeknadsland {
        val tomSøknadsland = Soeknadsland()

        if (soeknadsland == null || soeknadsland.landkoder.isEmpty()) {
            return tomSøknadsland
        }
        if ((nySakstype != EU_EOS) && (soeknadsland.isFlereLandUkjentHvilke() || (soeknadsland.landkoder.size != 1))) {
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
