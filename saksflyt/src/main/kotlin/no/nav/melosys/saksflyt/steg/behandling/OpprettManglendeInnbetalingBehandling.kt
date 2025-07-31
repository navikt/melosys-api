package no.nav.melosys.saksflyt.steg.behandling

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

    override fun inngangsSteg() = ProsessSteg.OPPRETT_MANGLENDE_INNBETALING_BEHANDLING

    override fun utfør(prosessinstans: Prosessinstans) {
        val fakturaserieReferanse = prosessinstans.getData(ProsessDataKey.FAKTURASERIE_REFERANSE)

        val sisteResultatMedReferanse = behandlingsresultatService
            .finnAlleBehandlingsresultatMedFakturaserieReferanse(fakturaserieReferanse)
            .sortedByDescending { it.registrertDato }
            .ifEmpty {
                throw FunksjonellException("Finner ikke behandlingsresultat med fakturaserie-referanse: $fakturaserieReferanse")
            }.first()

        val fagsak = behandlingService.hentBehandling(sisteResultatMedReferanse.id)?.fagsak
            ?: throw FunksjonellException("Fagsak er ikke tilstede for behandlingsresultat id: ${sisteResultatMedReferanse.id}")

        if (sisteResultatMedReferanse.medlemskapsperioder.isNotEmpty() && sisteResultatMedReferanse.medlemskapsperioder.all { it.erPliktig() }) {
            throw FunksjonellException("Det skal ikke opprettes behandling ved manglende innbetaling av avgift for pliktig medlemskap")
        }

        val mottaksdato = prosessinstans.getData(ProsessDataKey.MOTTATT_DATO, LocalDate::class.java)

        if (fagsak.harAktivBehandlingIkkeÅrsavregning()) {
            håndterAktivBehandling(fagsak, prosessinstans, mottaksdato)
        } else {
            lagNyBehandling(prosessinstans, fagsak, mottaksdato)
        }
    }

    private fun håndterAktivBehandling(
        fagsak: Fagsak,
        prosessinstans: Prosessinstans,
        mottaksdato: LocalDate?
    ) {
        val aktivBehandling = fagsak.hentAktivBehandlingIkkeÅrsavregning()
        when {
            aktivBehandling.erManglendeInnbetalingTrygdeavgift() -> {
                prosessinstans.behandling = aktivBehandling
                return
            }

            aktivBehandling.erNyVurdering() && aktivBehandling.opprinneligBehandling != null -> {
                aktivBehandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
                val manglendeInnbetalingFrist = aktivBehandling.utledBehandlingsfrist(mottaksdato)
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
                lagNyBehandling(prosessinstans, aktivBehandling.fagsak, mottaksdato)
                return
            }

            else -> throw FunksjonellException("Har ikke støtte for aktiv behandling: ${aktivBehandling.id}")
        }
    }

    private fun lagNyBehandling(prosessinstans: Prosessinstans, fagsak: Fagsak, mottaksDato: LocalDate?) {
        val behandlingBruktForReplikering = saksbehandlingRegler.finnBehandlingSomKanReplikeres(fagsak)
            ?: throw FunksjonellException(
                "Finner ikke behandling som skal brukes til replikering. fra fagsak: ${fagsak.saksnummer}" +
                    "Forventer at behandling som bestilte fakturering kan bli replikert fra."
            )

        val nyBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(
            behandlingBruktForReplikering,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )
        nyBehandling.settBehandlingsårsak(
            Behandlingsaarsak(Behandlingsaarsaktyper.MELDING_OM_MANGLENDE_INNBETALING, null, mottaksDato)
        )
        nyBehandling.behandlingsfrist = nyBehandling.utledBehandlingsfrist(mottaksDato)

        prosessinstans.behandling = nyBehandling
    }
}
