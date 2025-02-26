package no.nav.melosys.service.lovligekombinasjoner

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
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
class LovligeKombinasjonerSaksbehandlingService(
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
     * Henter mulige behandlingstyper for knytting til eksisterende sak
     *
     * @param hovedpart         Den valgte hovedpart knyttet til fagsaken.
     * @param saksnummer        Saksnummer til eksisterende fagsak
     * @param behandlingstema   Valgt behandlingstema.
     *
     * Brukt ved knytting til eksisterende sak.
     */
    fun hentMuligeBehandlingstyperForKnyttTilSak(
        hovedpart: Aktoersroller,
        saksnummer: String,
        behandlingstema: Behandlingstema?
    ): Set<Behandlingstyper> {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val sisteBehandling = fagsak.hentSistRegistrertBehandling()

        // Get base allowed types based on the fagsak's type and theme
        val behandlingstyper = hentMuligeBehandlingstyper(
            hovedpart,
            fagsak.type,
            fagsak.tema,
            behandlingstema,
            sisteBehandling
        ).toMutableSet()

        // Remove "FØRSTEGANG" if the last treatment is inactive
        if (sisteBehandling.erInaktiv()) {
            behandlingstyper.remove(Behandlingstyper.FØRSTEGANG)
        }
        // Remove "MANGLENDE_INNBETALING_TRYGDEAVGIFT" if no treatment is missing payment
        if (fagsak.behandlinger.none { it.erManglendeInnbetalingTrygdeavgift() }) {
            behandlingstyper.remove(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
        }
        // Optionally add "ÅRSAVREGNING" if the feature is enabled and any treatment qualifies
        if (unleash.isEnabled(ToggleName.MELOSYS_ÅRSAVREGNING) &&
            fagsak.behandlinger.any { it.tema in ARSAVREGNING_THEMES }
        ) {
            behandlingstyper.add(Behandlingstyper.ÅRSAVREGNING)
        }
        return behandlingstyper
    }

    /**
     * Henter mulige behandlingstyper for opprettelse av ny behandling og sak
     *
     * @param hovedpart         Den valgte hovedpart knyttet til fagsaken.
     * @param sakstype          Valgt sakstype.
     * @param sakstema          Valgt sakstema.
     * @param behandlingstema    Valgt behandlingstema.
     */
    fun hentMuligeBehandlingstyper(
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema?,
    ): Set<Behandlingstyper> {
        return hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, null)
            .filter { it != Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT }.toSet()
    }

    /**
     * Henter mulige behandlingstyper for endring av eksisterende behandling
     * @param hovedpart         Den valgte hovedpart knyttet til fagsaken.
     * @param sakstype          Valgt sakstype.
     * @param sakstema          Valgt sakstema.
     * @param behandlingstema    Valgt behandlingstema.
     * @param saksnummer        Saksnummer
     *
     */
    fun hentMuligeBehandlingstyperForEndring(
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema?,
        saksnummer: String
    ): Set<Behandlingstyper> {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        val aktivBehandling = fagsak.hentAktivBehandlingIkkeÅrsavregning()
        val behandlingstyper =
            hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, null).toMutableSet()

        if (aktivBehandling.fagsak.behandlinger.size > 1) {
            behandlingstyper.remove(Behandlingstyper.FØRSTEGANG)
        }
        if (aktivBehandling.erManglendeInnbetalingTrygdeavgift()) {
            return mutableSetOf(aktivBehandling.type)
        }
        return behandlingstyper
    }


    private fun hentMuligeBehandlingstyper(
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema?,
        sisteBehandling: Behandling?
    ): Set<Behandlingstyper> {
        // If there is an active treatment, check for a specific result
        if (sisteBehandling?.erAktiv() == true) {
            val sisteResultat = behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(sisteBehandling.id)
            return if (sisteResultat.erArtikkel16MedSendtAnmodningOmUnntak())
                setOf(Behandlingstyper.NY_VURDERING)
            else
                emptySet()
        }

        return when (hovedpart) {
            Aktoersroller.BRUKER -> {
                // For users, use a dedicated helper and filter out "ÅRSAVREGNING" if needed.
                val typer = behandlingstyperForBruker(
                    sakstype,
                    sakstema,
                    behandlingstema,
                    sisteBehandling?.tema,
                    sisteBehandling?.fagsak?.status
                )
                // Remove "ÅRSAVREGNING" if the toggle is disabled.
                if (!unleash.isEnabled(ToggleName.MELOSYS_ÅRSAVREGNING)) {
                    typer.filterNot { it == Behandlingstyper.ÅRSAVREGNING }.toSet()
                } else {
                    typer
                }
            }
            Aktoersroller.VIRKSOMHET -> {
                // For companies, derive allowed types directly from the combinations.
                LovligeSakskombinasjoner.muligeSaksKombinasjonerVirksomhet
                    .getOrDefault(sakstype, emptySet())
                    .filter { it.sakstema == sakstema }
                    .flatMap { it.behandlingstemaBehandlingstyperKombinasjoner }
                    .filter { behandlingstema in it.behandlingsTemaer }
                    .flatMap { it.behandlingsTyper }
                    .toSet()
            }
            else -> throw FunksjonellException("Støtter ikke andre hovedparter i sak enn Bruker og Virksomhet")
        }
    }

    private fun behandlingstyperForBruker(
        sakstype: Sakstyper?,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema?,
        sistBehandlingstema: Behandlingstema?,
        sistSaksstatus: Saksstatuser?
    ): Set<Behandlingstyper> {
        // Get the base set of allowed types for a user.
        var behandlingstyper = LovligeSakskombinasjoner.muligeSaksKombinasjonerBruker
            .getOrDefault(sakstype, emptySet())
            .filter { it.sakstema == sakstema }
            .flatMap { it.behandlingstemaBehandlingstyperKombinasjoner }
            .filter { behandlingstema in it.behandlingsTemaer }
            .flatMap { it.behandlingsTyper }
            .toMutableSet()

        // Override allowed types if the previous treatment's theme is one of the special ones.
        if (sistBehandlingstema in SPECIAL_BEHANDLINGSTEMA_SET) {
            behandlingstyper = mutableSetOf(
                Behandlingstyper.NY_VURDERING,
                Behandlingstyper.KLAGE,
                Behandlingstyper.HENVENDELSE
            )
        }
        // If the last case status is one of the special statuses, limit to "HENVENDELSE".
        if (sistSaksstatus in SPECIAL_SAKSSTATUS_SET) {
            behandlingstyper = mutableSetOf(Behandlingstyper.HENVENDELSE)
        }
        // Remove "KLAGE" if the feature toggle is not enabled.
        if (!unleash.isEnabled(ToggleName.BEHANDLINGSTYPE_KLAGE)) {
            behandlingstyper.remove(Behandlingstyper.KLAGE)
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
        validerBehandlingstype(hovedpart, sakstype, sakstema, behandlingstema, behandlingstype)
    }

    fun validerBehandlingstemaOgBehandlingstypeForAndregangsbehandling(
        fagsak: Fagsak,
        sistBehandling: Behandling,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ) {
        validerBehandlingstema(fagsak.hovedpartRolle, fagsak.type, fagsak.tema, behandlingstema, null, sistBehandling.tema)
        validerBehandlingstypeForKnyttTilSak(
            fagsak.hovedpartRolle,
            fagsak.saksnummer,
            behandlingstema,
            behandlingstype,
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
        hovedpart: Aktoersroller,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
    ) {
        if (!hentMuligeBehandlingstyper(
                hovedpart,
                sakstype,
                sakstema,
                behandlingstema,
            ).contains(behandlingstype)
        ) {
            throw FunksjonellException("$behandlingstype er ikke en lovlig behandlingstype med de andre valgte verdiene")
        }
    }

    private fun validerBehandlingstypeForKnyttTilSak(
        hovedpart: Aktoersroller,
        saksnummer: String,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ) {
        if (!hentMuligeBehandlingstyperForKnyttTilSak(
                hovedpart,
                saksnummer,
                behandlingstema,
            ).contains(behandlingstype)
        ) {
            throw FunksjonellException("$behandlingstype er ikke en lovlig behandlingstype med de andre valgte verdiene")
        }
    }

    companion object {
        // Themes that trigger a complete override on allowed types for a user
        private val SPECIAL_BEHANDLINGSTEMA_SET = setOf(
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
            Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
            Behandlingstema.BESLUTNING_LOVVALG_NORGE,
            Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND,
            Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        )

        // Case statuses that force a reduced set of allowed types
        private val SPECIAL_SAKSSTATUS_SET = setOf(
            Saksstatuser.HENLAGT,
            Saksstatuser.HENLAGT_BORTFALT,
            Saksstatuser.AVSLUTTET,
            Saksstatuser.OPPHØRT,
            Saksstatuser.ANNULLERT
        )

        // Themes that qualify for an "ÅRSAVREGNING" type
        private val ARSAVREGNING_THEMES = setOf(
            Behandlingstema.YRKESAKTIV,
            Behandlingstema.UTSENDT_ARBEIDSTAKER,
            Behandlingstema.UTSENDT_SELVSTENDIG,
            Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY,
            Behandlingstema.ARBEID_FLERE_LAND,
            Behandlingstema.ARBEID_KUN_NORGE
        )
    }
}
