package no.nav.melosys.saksflyt.steg.behandling

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OpprettManglendeInnbetalingBehandling(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val saksbehandlingRegler: SaksbehandlingRegler,
    private val oppgaveService: OppgaveService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_MANGLENDE_INNBETALING_BEHANDLING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val fakturaserieReferanse = prosessinstans.getData(ProsessDataKey.FAKTURASERIE_REFERANSE)
        val mottaksDato = prosessinstans.getData(ProsessDataKey.MOTTATT_DATO, LocalDate::class.java)

        val behandlingsresultater = behandlingsresultatService
            .finnAlleBehandlingsresultatMedFakturaserieReferanse(fakturaserieReferanse)

        if (behandlingsresultater.isEmpty()) {
            throw FunksjonellException("Finner ikke behandlingsresultat med fakturaserie-referanse: $fakturaserieReferanse")
        }

        val førsteBehandlingsresultat = behandlingsresultater.first()

        val fagsak = behandlingService.hentBehandling(behandlingsresultater.first().id)?.fagsak
            ?: throw FunksjonellException("Fagsak er ikke tilstede for behandlingsresultat id: ${behandlingsresultater.first().id}")

        if (førsteBehandlingsresultat.medlemskapsperioder.any { it.erPliktig() }) {
            val behandlingMedFattetVedtak = saksbehandlingRegler.finnBehandlingSomKanReplikeres(fagsak)
            if (behandlingMedFattetVedtak != null) {
                prosessinstans.behandling = behandlingMedFattetVedtak
                return
            }
        }

        if (fagsak.harAktivBehandlingIkkeÅrsavregning()) {
            val aktivBehandling = fagsak.hentAktivBehandlingIkkeÅrsavregning()
            håndterAktivBehandling(aktivBehandling, prosessinstans, mottaksDato)
            return
        }

        lagNyBehandling(prosessinstans, fagsak, mottaksDato)
    }

    private fun håndterAktivBehandling(
        aktivBehandling: Behandling,
        prosessinstans: Prosessinstans,
        mottaksDato: LocalDate?
    ) {
        when {
            aktivBehandling.erManglendeInnbetalingTrygdeavgift() -> {
                prosessinstans.behandling = aktivBehandling
                return
            }

            aktivBehandling.erNyVurdering() && aktivBehandling.opprinneligBehandling != null -> {
                aktivBehandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                val manglendeInnbetalingFrist = Behandling.utledBehandlingsfrist(aktivBehandling, mottaksDato)
                if (manglendeInnbetalingFrist.isBefore(aktivBehandling.behandlingsfrist)) {
                    aktivBehandling.behandlingsfrist = manglendeInnbetalingFrist
                }
                prosessinstans.behandling = aktivBehandling
                return
            }

            aktivBehandling.type in listOf(Behandlingstyper.HENVENDELSE, Behandlingstyper.NY_VURDERING) &&
                aktivBehandling.opprinneligBehandling == null -> {
                behandlingService.avsluttBehandling(aktivBehandling.id)
                behandlingsresultatService.oppdaterBehandlingsresultattype(aktivBehandling.id, Behandlingsresultattyper.AVBRUTT)
                oppgaveService.ferdigstillOppgaveMedBehandlingID(aktivBehandling.id)
                lagNyBehandling(prosessinstans, aktivBehandling.fagsak, mottaksDato)
                return
            }

            else -> throw FunksjonellException("Har ikke støtte for aktiv behandling: ${aktivBehandling.id}")
        }
    }

    private fun lagNyBehandling(prosessinstans: Prosessinstans, fagsak: Fagsak, mottaksDato: LocalDate?) {
        val behandlingBruktForReplikering = saksbehandlingRegler.finnBehandlingSomKanReplikeres(fagsak)
            ?: throw FunksjonellException(
                "Finner ikke behandling som skal brukes til replikering. fra fagsag: ${fagsak.saksnummer}" +
                    "Forventer at behandling som bestilte fakturering kan bli replikert fra."
            )

        val nyBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(
            behandlingBruktForReplikering,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )
        nyBehandling.settBehandlingsårsak(
            Behandlingsaarsak(Behandlingsaarsaktyper.MELDING_OM_MANGLENDE_INNBETALING, null, mottaksDato)
        )
        nyBehandling.behandlingsfrist = Behandling.utledBehandlingsfrist(nyBehandling, mottaksDato)

        prosessinstans.behandling = nyBehandling
    }
}
