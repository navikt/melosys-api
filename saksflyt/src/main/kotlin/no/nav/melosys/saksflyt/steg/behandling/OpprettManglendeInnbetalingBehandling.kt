package no.nav.melosys.saksflyt.steg.behandling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsaarsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OpprettManglendeInnbetalingBehandling(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val saksbehandlingRegler: SaksbehandlingRegler
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_MANGLENDE_INNBETALING_BEHANDLING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val fakturaserieReferanse = prosessinstans.getData(ProsessDataKey.FAKTURASERIE_REFERANSE)
        val mottaksDato = prosessinstans.getData(ProsessDataKey.MOTTATT_DATO, LocalDate::class.java)

        val behandlingsresultater =
            behandlingsresultatService.finnAlleBehandlingsresultatMedFakturaserieReferanse(fakturaserieReferanse)

        if (behandlingsresultater.isEmpty()) {
            throw FunksjonellException("Finner ikke behandlingsresultat med fakturaserie-referanse: $fakturaserieReferanse")
        }

        val fagsak = behandlingService.hentBehandling(behandlingsresultater.first().id).fagsak

        if (fagsak.harAktivBehandling()) {
            // TODO: Ta hensyn til at det kan eksistere en åpen behadnling fra før. Gjøres på egen oppgave.
            // MELOSYS-6187. Husk tester!
            throw FunksjonellException("Her kommer Rune å fikser stuff")
        }

        val behandlingBruktForReplikering = saksbehandlingRegler.finnBehandlingSomKanReplikeres(fagsak)
            ?: throw FunksjonellException(
                "Finner ikke behandling som skal brukes til replikering. " +
                    "Forventer at behandling som bestilte fakturering kan bli replikert fra."
            )

        val nyBehandling = behandlingService.replikerBehandlingOgBehandlingsresultat(
            behandlingBruktForReplikering,
            Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
        )
        nyBehandling.settBehandlingsårsak(Behandlingsaarsak(Behandlingsaarsaktyper.MELDING_OM_MANGLENDE_INNBETALING, null, mottaksDato))
        nyBehandling.behandlingsfrist = Behandling.utledBehandlingsfrist(nyBehandling, mottaksDato)


        prosessinstans.behandling = nyBehandling
    }
}
