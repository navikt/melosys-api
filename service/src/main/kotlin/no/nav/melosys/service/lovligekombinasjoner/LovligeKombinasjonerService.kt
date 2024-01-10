package no.nav.melosys.service.lovligekombinasjoner

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Service

@Service
class LovligeKombinasjonerService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val unleash: Unleash
) {

    /**
     * Henter mulige sakstyper
     *
     * @param saksnummer Saksnummer til eksisterende fagsak. (default = null)
     * Brukt for å sjekke om eksisterende fagsak kan endre sakstype.
     */
    fun hentMuligeSakstyper(saksnummer: String?): Set<Sakstyper> {
        if (saksnummer == null || fagsakService.hentFagsak(saksnummer).kanEndreTypeOgTema()) {
            return LovligeSakskombinasjoner.ALLE_MULIGE_SAKSTYPER
        }
        return emptySet()

    }


    /**
     * Henter mulige sakstemaer
     *
     * @param hovedpart  Hovedpart knyttet til fagsaken. (default = null)
     * Sender vi ikke inn hovedpart returnerer vi mulige sakstemaer for alle støttede hovedparter.
     * @param sakstype   Allerede valgt sakstype.
     * @param saksnummer Saksnummer til eksisterende fagsak. (default = null)
     * Brukt for å sjekke om eksisterende fagsak kan endre sakstema.
     */
    fun hentMuligeSakstemaer(hovedpart: Aktoersroller?, sakstype: Sakstyper?, saksnummer: String?): Set<Sakstemaer> {
        if (hovedpart == null) {
            return hentMuligeSakstemaer(Aktoersroller.BRUKER, sakstype, saksnummer) +
                hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, sakstype, saksnummer)
        }

        if (saksnummer != null && !fagsakService.hentFagsak(saksnummer).kanEndreTypeOgTema()) {
            return emptySet()
        }

        return when (hovedpart) {
            Aktoersroller.BRUKER -> LovligeSakskombinasjoner.muligeSaksKombinasjonerBruker.getOrDefault(sakstype, emptySet())
                .map { it.sakstema }.toSet()

            Aktoersroller.VIRKSOMHET -> LovligeSakskombinasjoner.muligeSaksKombinasjonerVirksomhet.getOrDefault(sakstype, emptySet())
                .map { it.sakstema }.toSet()

            else -> throw FunksjonellException("Støtter ikke andre hovedparter i sak enn Bruker og Virksomhet")
        }
    }


    /**
     * Henter mulige behandlingstemaer
     *
     * @param hovedpart           Hovedpart knyttet til fagsaken. (default = null)
     * Hvis null returnerer vi mulige behandlingstemaer for alle støttede hovedparter samt
     * mulige behandlingstemaer for SED. (MELOSYS-5223)
     * @param sakstype            Allerede valgt sakstype.
     * @param sakstema            Allerede valgt sakstema.
     * @param aktivBehandlingID   Nåværende behandling i fagsaken. (default = null) Brukt ved endring av sak.
     * @param sistBehandlingstema Behandlingstema til forrige behandling i fagsaken. (default = null)
     * Brukt ved knytting til eksisterende sak.
     */
    fun hentMuligeBehandlingstemaer(
        hovedpart: Aktoersroller?,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        aktivBehandlingID: Long?,
        sistBehandlingstema: Behandlingstema?,
    ): Set<Behandlingstema> {
        if (hovedpart == null) {
            return hentMuligeBehandlingstemaer(Aktoersroller.BRUKER, sakstype, sakstema, aktivBehandlingID, sistBehandlingstema) +
                hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, sakstype, sakstema, aktivBehandlingID, sistBehandlingstema) +
                hentMuligeBehandlingstemaerSED(sakstype, sakstema)
        }

        return when (hovedpart) {
            Aktoersroller.BRUKER -> behandlingstemaForBruker(sakstype, sakstema, aktivBehandlingID, sistBehandlingstema)
            Aktoersroller.VIRKSOMHET -> LovligeSakskombinasjoner.muligeSaksKombinasjonerVirksomhet.getOrDefault(sakstype, emptySet())
                .filter { it.sakstema == sakstema }
                .flatMap { it.behandlingstemaBehandlingstyperKombinasjoner }
                .flatMap { it.behandlingsTemaer }
                .toSet()

            else -> throw FunksjonellException("Støtter ikke andre hovedparter i sak enn Bruker og Virksomhet")
        }
    }

    private fun behandlingstemaForBruker(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        aktivBehandlingID: Long?,
        sistBehandlingstema: Behandlingstema?
    ): Set<Behandlingstema> {
        val behandlingstemaer = LovligeSakskombinasjoner.muligeSaksKombinasjonerBruker.getOrDefault(sakstype, emptySet())
            .filter { it.sakstema == sakstema }
            .flatMap { it.behandlingstemaBehandlingstyperKombinasjoner }
            .flatMap { it.behandlingsTemaer }
            .toSet()

        if (sistBehandlingstema in setOf(
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            )
        ) {
            return setOf(sistBehandlingstema!!)
        }

        val aktivBehandling = if (aktivBehandlingID != null) behandlingService.hentBehandling(aktivBehandlingID) else null
        if (aktivBehandling?.erManglendeInnbetalingTrygdeavgift() == true) {
            return setOf(aktivBehandling.tema)
        }

        return behandlingstemaer
    }

    private fun hentMuligeBehandlingstemaerSED(sakstype: Sakstyper, sakstema: Sakstemaer): Set<Behandlingstema> {
        if (sakstype == Sakstyper.EU_EOS) {
            return LovligeSakskombinasjoner.EU_EOS_SED_BEHANDLINGSTEMA.getOrDefault(sakstema, emptySet())
        }
        return emptySet()
    }


    /**
     * Henter mulige behandlingstyper
     *
     * @param hovedpart         Hovedpart knyttet til fagsaken.
     * @param sakstype          Allerede valgt sakstype.
     * @param sakstema          Allerede valgt sakstema.
     * @param behandlingstema   Allerede valgt behandlingstema.
     * @param aktivBehandlingID Nåværende behandling i fagsaken. (default = null) Brukt ved endring av sak.
     * @param sisteBehandlingID Forrige behandling i fagsaken. (default = null)
     * Brukt ved knytting til eksisterende sak.
     */
    fun hentMuligeBehandlingstyper(
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema?,
        aktivBehandlingID: Long?,
        sisteBehandlingID: Long?
    ): Set<Behandlingstyper> {
        val aktivBehandling = if (aktivBehandlingID != null) behandlingService.hentBehandling(aktivBehandlingID) else null
        val sisteBehandling = if (sisteBehandlingID != null) behandlingService.hentBehandling(sisteBehandlingID) else null
        val sisteBehandlingsresultat =
            if (sisteBehandlingID != null) behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(sisteBehandlingID) else null

        return hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, aktivBehandling, sisteBehandling, sisteBehandlingsresultat)
    }

    fun hentMuligeBehandlingstyper(
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema?,
        aktivBehandling: Behandling?,
        sisteBehandling: Behandling?,
        sisteBehandlingsresultat: Behandlingsresultat?
    ): Set<Behandlingstyper> {
        if (sisteBehandling?.erAktiv() == true) {
            return if (sisteBehandlingsresultat!!.erArtikkel16MedSendtAnmodningOmUnntak()) setOf(Behandlingstyper.NY_VURDERING) else emptySet()
        }

        return when (hovedpart) {
            Aktoersroller.BRUKER -> behandlingstyperForBruker(
                sakstype,
                sakstema,
                behandlingstema,
                aktivBehandling,
                sisteBehandling,
                sisteBehandling?.tema,
                sisteBehandling?.fagsak?.status
            )

            Aktoersroller.VIRKSOMHET -> LovligeSakskombinasjoner.muligeSaksKombinasjonerVirksomhet.getOrDefault(sakstype, emptySet())
                .filter { it.sakstema == sakstema }
                .flatMap { it.behandlingstemaBehandlingstyperKombinasjoner }
                .filter { behandlingstema in it.behandlingsTemaer }
                .flatMap { it.behandlingsTyper }
                .toSet()

            else -> throw FunksjonellException("Støtter ikke andre hovedparter i sak enn Bruker og Virksomhet")
        }
    }

    private fun behandlingstyperForBruker(
        sakstype: Sakstyper?, sakstema: Sakstemaer,
        behandlingstema: Behandlingstema?, aktivBehandling: Behandling?, sisteBehandling: Behandling?,
        sistBehandlingstema: Behandlingstema?, sistSaksstatus: Saksstatuser?
    ): Set<Behandlingstyper> {
        var behandlingstyper = LovligeSakskombinasjoner.muligeSaksKombinasjonerBruker.getOrDefault(sakstype, emptySet())
            .filter { it.sakstema == sakstema }
            .flatMap { it.behandlingstemaBehandlingstyperKombinasjoner }
            .filter { behandlingstema in it.behandlingsTemaer }
            .flatMap { it.behandlingsTyper }
            .toMutableSet()

        if (sistBehandlingstema in setOf(
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE,
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
                Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
            )
        ) {
            behandlingstyper = mutableSetOf(Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE, Behandlingstyper.HENVENDELSE)
        }
        if (aktivBehandling != null && aktivBehandling.fagsak.behandlinger.size > 1) {
            behandlingstyper.remove(Behandlingstyper.FØRSTEGANG)
        }
        if (sisteBehandling?.erInaktiv() == true) {
            behandlingstyper.remove(Behandlingstyper.FØRSTEGANG)
        }
        if (sistSaksstatus in setOf(Saksstatuser.HENLAGT, Saksstatuser.HENLAGT_BORTFALT, Saksstatuser.AVSLUTTET)) {
            behandlingstyper = mutableSetOf(Behandlingstyper.HENVENDELSE)
        }
        if (!unleash.isEnabled(ToggleName.BEHANDLINGSTYPE_KLAGE)) {
            behandlingstyper.remove(Behandlingstyper.KLAGE)
        }
        if (unleash.isEnabled(ToggleName.SAKSBEHANDLING_MANGLENDE_INNBETALING)) {
            if (aktivBehandling?.erManglendeInnbetalingTrygdeavgift() == true) {
                behandlingstyper = mutableSetOf(aktivBehandling.type)
            } else if (sisteBehandling?.fagsak?.behandlinger?.any { it.erManglendeInnbetalingTrygdeavgift() } != true) {
                behandlingstyper.remove(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
            }
        } else {
            behandlingstyper.remove(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
        }

        return behandlingstyper
    }

    /**
     * Henter mulige behandlingsårsaker
     *
     * @param behandlingstype   Allerede valgt behandlingstype.
     */
    fun hentMuligeBehandlingsårsaktyper(behandlingstype: Behandlingstyper): List<Behandlingsaarsaktyper> {
        if (behandlingstype == Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) {
            return listOf(Behandlingsaarsaktyper.MELDING_OM_MANGLENDE_INNBETALING)
        }
        return listOf(
            Behandlingsaarsaktyper.SØKNAD,
            Behandlingsaarsaktyper.SED,
            Behandlingsaarsaktyper.HENVENDELSE,
            Behandlingsaarsaktyper.FRITEKST
        )
    }

    fun hentMuligeBehandlingStatuser(): Set<Behandlingsstatus> = LovligeBehandlingstatus.ALLE_MULIGE_BEHANDLINGSTATUSER

    fun validerNyStatusMulig(behandling: Behandling, status: Behandlingsstatus) {
        if (!hentMuligeBehandlingStatuser().contains(status)) {
            throw FunksjonellException("Behandlingen kan ikke endres til status $status. Gyldige statuser for behandling ${behandling.id} er ${hentMuligeBehandlingStatuser()}")
        }
    }

    fun validerOpprettelseOgEndring(
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ) = validerOpprettelseOgEndring(null, hovedpart, sakstype, sakstema, behandlingstema, behandlingstype)


    fun validerOpprettelseOgEndring(
        aktivBehandling: Behandling?,
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ) {
        validerSakstema(hovedpart, sakstype, sakstema)
        validerBehandlingstema(hovedpart, sakstype, sakstema, behandlingstema, aktivBehandling, null)
        validerBehandlingstype(hovedpart, sakstype, sakstema, behandlingstema, behandlingstype, aktivBehandling, null, null)
    }

    fun validerBehandlingstemaOgBehandlingstypeForAndregangsbehandling(
        fagsak: Fagsak,
        sistBehandling: Behandling,
        sistBehandlingsresultat: Behandlingsresultat?,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ) {
        validerBehandlingstema(fagsak.hovedpartRolle, fagsak.type, fagsak.tema, behandlingstema, null, sistBehandling.tema)
        validerBehandlingstype(
            fagsak.hovedpartRolle,
            fagsak.type,
            fagsak.tema,
            behandlingstema,
            behandlingstype,
            null,
            sistBehandling,
            sistBehandlingsresultat
        )
    }

    private fun validerSakstema(hovedpart: Aktoersroller?, sakstype: Sakstyper, sakstema: Sakstemaer) {
        if (!hentMuligeSakstemaer(hovedpart, sakstype, null).contains(sakstema)) {
            throw FunksjonellException("$sakstema er ikke et lovlig sakstema med de andre valgte verdiene")
        }
    }

    private fun validerBehandlingstema(
        hovedpart: Aktoersroller?,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        aktivBehandling: Behandling?,
        sistBehandlingstema: Behandlingstema?
    ) {
        if (!hentMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, aktivBehandling?.id, sistBehandlingstema).contains(behandlingstema)) {
            throw FunksjonellException("$behandlingstema er ikke et lovlig behandlingstema med de andre valgte verdiene")
        }
    }

    private fun validerBehandlingstype(
        hovedpart: Aktoersroller, sakstype: Sakstyper, sakstema: Sakstemaer,
        behandlingstema: Behandlingstema, behandlingstype: Behandlingstyper,
        aktivBehandling: Behandling?, sistBehandling: Behandling?, sistBehandlingsresultat: Behandlingsresultat?
    ) {
        if (!hentMuligeBehandlingstyper(
                hovedpart,
                sakstype,
                sakstema,
                behandlingstema,
                aktivBehandling,
                sistBehandling,
                sistBehandlingsresultat
            ).contains(behandlingstype)
        ) {
            throw FunksjonellException("$behandlingstype er ikke en lovlig behandlingstype med de andre valgte verdiene")
        }
    }
}
