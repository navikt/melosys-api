package no.nav.melosys.service.sak

import no.finn.unleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.VedtakMetadataLagretEvent
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
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
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        behandlingsstatus: Behandlingsstatus?,
        behandlingsfrist: LocalDate?
    ) {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        validerSak(fagsak)
        validerEndring(fagsak, sakstype, sakstema, behandlingstema, behandlingstype)

        val behandling = fagsak.hentAktivBehandling()
        if (fagsak.type == sakstype && fagsak.tema == sakstema) {
            behandlingService.endreBehandling(behandling.id, behandlingstype, behandlingstema, behandlingsstatus, behandlingsfrist)
            return
        }

        if (SaksbehandlingRegler.harTomFlyt(sakstype, sakstema, behandlingstype, behandlingstema, unleash.isEnabled("melosys.folketrygden.mvp"))) {
            mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id).ifPresent { mottatteOpplysningerService.slettOpplysninger(behandling.id) }
        } else {
            gjenopprettMottatteOpplysninger(sakstype, behandling)
        }

        fagsakService.oppdaterSakstype(saksnummer, sakstype)
        fagsakService.oppdaterSakstema(saksnummer, sakstema)
        behandlingService.endreBehandling(behandling.id, behandlingstype, behandlingstema, behandlingsstatus, behandlingsfrist)

        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)
        // TODO oppdater oppgave
        applicationEventPublisher.publishEvent(VedtakMetadataLagretEvent(1L))
    }

    private fun validerSak(fagsak: Fagsak) {
        if (!fagsak.kanEndres()) {
            throw FunksjonellException("Sak ${fagsak.saksnummer} kan ikke endres")
        }
    }

    private fun validerEndring(
        fagsak: Fagsak,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ) {
        lovligeKombinasjonerService.validerBehandlingstemaOgBehandlingstypeForOpprettelse(
            fagsak.hovedpartRolle,
            sakstype,
            sakstema,
            behandlingstema,
            behandlingstype
        )
    }

    private fun gjenopprettMottatteOpplysninger(
        sakstype: Sakstyper, behandling: Behandling
    ) {
        val mottatteOpplysninger = mottatteOpplysningerService.hentMottatteOpplysninger(behandling.id)
        if (sakstype == Sakstyper.EU_EOS && !unleash.isEnabled("melosys.tom_periode_og_land")) {
            validerMottatteOpplysninger(mottatteOpplysninger)
        }

        mottatteOpplysningerService.slettOpplysninger(behandling.id)
        mottatteOpplysningerService.opprettSøknad(
            behandling,
            mottatteOpplysninger?.mottatteOpplysningerData?.periode ?: Periode(),
            mottatteOpplysninger?.mottatteOpplysningerData?.soeknadsland ?: Soeknadsland()
        )
    }

    private fun validerMottatteOpplysninger(mottatteOpplysninger: MottatteOpplysninger?) {
        val mottatteOpplysningerdata = mottatteOpplysninger?.mottatteOpplysningerData
        if (mottatteOpplysningerdata == null || manglerPeriode(mottatteOpplysningerdata) || manglerSøknadsland(
                mottatteOpplysningerdata
            )
        ) {
            throw FunksjonellException("Du må legge inn periode og land i flyten for å kunne bytte til sakstype EU/EØS")
        }
    }

    private fun manglerPeriode(mottatteOpplysningerdata: MottatteOpplysningerData) =
        (mottatteOpplysningerdata.periode == null || mottatteOpplysningerdata.periode.fom == null)

    private fun manglerSøknadsland(mottatteOpplysningerdata: MottatteOpplysningerData) =
        (mottatteOpplysningerdata.soeknadsland == null || mottatteOpplysningerdata.soeknadsland.erUkjenteEllerAlleEosLand)
}
