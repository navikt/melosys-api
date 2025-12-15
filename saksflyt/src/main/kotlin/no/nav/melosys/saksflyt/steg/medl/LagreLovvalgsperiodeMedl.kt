package no.nav.melosys.saksflyt.steg.medl

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.springframework.stereotype.Component
import kotlin.jvm.optionals.getOrNull


private val log = KotlinLogging.logger { }

@Component
class LagreLovvalgsperiodeMedl(
    private val behandlingsresultatService: BehandlingsresultatService,
    private val medlPeriodeService: MedlPeriodeService,
    private val saksbehandlingRegler: SaksbehandlingRegler,
    private val lovvalgsperiodeService: LovvalgsperiodeService
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg = ProsessSteg.LAGRE_LOVVALGSPERIODE_MEDL

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.hentBehandling
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val erAnnullering = prosessinstans.type == ProsessType.ANNULLER_SAK

        if (erIkkeGodkjentRegistreringUnntakFraMedlemskap(behandling, behandlingsresultat.utfallRegistreringUnntak) ||
            (erUnntakTuristSkip(behandlingsresultat) && behandling.erFørstegangsvurdering()) ||
            (behandling.erEøsPensjonist() || !erBehandlingEøsEllerTrygdeavtale(behandling))
        ) {
            return
        }

        val lovvalgsperiode = behandlingsresultat.hentLovvalgsperiode()

        if (behandling.erNyVurdering()) {
            lovvalgsperiode.medlPeriodeID = finnOpprinneligMedlPeriodeID(behandling)
        }

        if (erAnnullering && behandling.erNyVurdering()) lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT

        oppdaterLovvalgsperiode(behandling, lovvalgsperiode, erAnnullering)
    }

    private fun erBehandlingEøsEllerTrygdeavtale(behandling: Behandling): Boolean =
        listOf(Sakstyper.EU_EOS, Sakstyper.TRYGDEAVTALE).contains(behandling.fagsak.type)


    private fun erUnntakTuristSkip(behandlingsresultat: Behandlingsresultat): Boolean =
        behandlingsresultat.oppfyllerVilkår(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP)

    private fun erIkkeGodkjentRegistreringUnntakFraMedlemskap(
        behandling: Behandling,
        utfallregistreringunntak: Utfallregistreringunntak?
    ): Boolean =
        saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) &&
            utfallregistreringunntak == Utfallregistreringunntak.IKKE_GODKJENT

    private fun finnOpprinneligMedlPeriodeID(behandling: Behandling): Long? {
        val opprinneligBehandling = behandling.opprinneligBehandling ?: run {
            log.warn { "opprinneligBehandling er null for behandling ${behandling.id}" }
            return null
        }


        val opprinneligResultat = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)

        return opprinneligResultat.finnLovvalgsperiode().getOrNull()?.medlPeriodeID
    }

    private fun oppdaterLovvalgsperiode(behandling: Behandling, lovvalgsperiode: Lovvalgsperiode, erAnnullering: Boolean = false) {
        if (lovvalgsperiode.erAvslått()) {
            if (lovvalgsperiode.medlPeriodeID != null) {
                medlPeriodeService.avvisPeriode(lovvalgsperiode.medlPeriodeID)
                if (erAnnullering) {
                    lovvalgsperiodeService.slettLovvalgsperiode(lovvalgsperiode.id!!)
                }
            }
        } else if (lovvalgsperiode.erInnvilget()) {
            opprettEllerOppdaterMedlPeriode(behandling, lovvalgsperiode)
        } else {
            throw FunksjonellException(
                "Ukjent eller ikke-eksisterende innvilgelsesresultat for en lovvalgsperiode: ${lovvalgsperiode.innvilgelsesresultat}"
            )
        }
    }

    private fun opprettEllerOppdaterMedlPeriode(behandling: Behandling, lovvalgsperiode: Lovvalgsperiode) {
        if (lovvalgsperiode.medlPeriodeID == null) {
            opprettMedlPeriode(behandling, lovvalgsperiode)
        } else {
            oppdaterMedlPeriode(behandling, lovvalgsperiode)
        }
    }

    private fun opprettMedlPeriode(behandling: Behandling, lovvalgsperiode: Lovvalgsperiode) {
        val behandlingID = behandling.id
        if (lovvalgsperiode.erArtikkel13() && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)) {
            medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandlingID)
        } else {
            medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandlingID)
        }
    }

    private fun oppdaterMedlPeriode(behandling: Behandling, lovvalgsperiode: Lovvalgsperiode) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        if (erUnntakTuristSkip(behandlingsresultat)) {
            medlPeriodeService.avvisPeriodeFeilregistrert(lovvalgsperiode.hentMedlPeriodeID())
        } else if (lovvalgsperiode.erArtikkel13() && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(
                behandling
            )
        ) {
            medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode)
        } else {
            medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode)
        }

    }
}
