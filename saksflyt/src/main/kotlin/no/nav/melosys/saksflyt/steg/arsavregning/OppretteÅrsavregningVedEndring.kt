package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = mu.KotlinLogging.logger { }

@Component
class OppretteÅrsavregningVedEndring(
    private val årsavregningService: ÅrsavregningService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val prosessInstansService: ProsessinstansService,
    private val unleash: Unleash
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg? {
        return ProsessSteg.OPPRETTE_AARSAVREGNING_ENDRING
    }

    // Er egne oppgaver som skal legge til FTRL.penjonist, EØS.pensjonist|offentlig-tjenesteperson år årsavregning er ok
    fun harTemaOgTypeSomSkalBehandles(behandling: Behandling, fagsak: Fagsak) =
        behandling.tema == Behandlingstema.YRKESAKTIV && fagsak.type == Sakstyper.FTRL

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            return
        }

        val behandling = prosessinstans.hentBehandling
        val fagsak = behandling.fagsak

        if (!harTemaOgTypeSomSkalBehandles(behandling, fagsak)) {
            return
        }

        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val potensielleÅrsavregningÅrNy: Set<Int> = hentPotensielleÅrsavregningÅrFraAvgiftsperioder(behandlingsresultat)

        if (behandling.erFørstegangsvurdering()) {
            opprettÅrsavregning(potensielleÅrsavregningÅrNy, behandling)
        } else if (behandling.erNyVurdering()) {
            val opprinneligBehandling = behandling.hentOpprinneligBehandling()
            val opprinneligBehandlingsresultat =
                behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)
            if (!harEndringerITidligereÅr(behandlingsresultat, opprinneligBehandlingsresultat)) {
                return
            }
            val potensielleÅrsavregningÅr: Set<Int> =
                hentPotensielleÅrsavregningÅrFraAvgiftsperioder(opprinneligBehandlingsresultat).union(
                    potensielleÅrsavregningÅrNy
                )
            opprettÅrsavregning(potensielleÅrsavregningÅr, behandling)
        }
    }

    private fun opprettÅrsavregning(
        potensielleÅrsavregningÅr: Set<Int>,
        behandling: Behandling
    ) {
        potensielleÅrsavregningÅr.forEach { potensieltÅr ->
            val harAktivÅrsavregningforÅr =
                årsavregningService.finnÅrsavregningerPåFagsak(
                    behandling.fagsak.saksnummer,
                    potensieltÅr,
                    Behandlingsresultattyper.IKKE_FASTSATT
                )
                    .isNotEmpty()
            if (!harAktivÅrsavregningforÅr) {
                log.info("Oppretter årsavregningsbehandling for år $potensieltÅr på saksnummer ${behandling.fagsak.saksnummer} basert på behandling ${behandling.id}")
                prosessInstansService.opprettArsavregningsBehandlingProsessflyt(
                    behandling.fagsak.saksnummer,
                    potensieltÅr.toString(),
                    Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE
                )
            }
        }
    }

    private fun hentPotensielleÅrsavregningÅrFraAvgiftsperioder(behandlingsresultat: Behandlingsresultat): Set<Int> =
        behandlingsresultat.finnAvgiftspliktigPerioder()
            .filter { it.erInnvilget() }
            .flatMap { periode ->
                val fom = periode.getFom() ?: return@flatMap emptySequence()
                val tom = periode.getTom()
                val inneværendeÅr = LocalDate.now().year

                val startÅr = fom.year
                val sluttÅr = tom?.year ?: inneværendeÅr

                (startÅr..sluttÅr).asSequence().filter { it < inneværendeÅr }
            }
            .toSet()

    fun harEndringerITidligereÅr(
        behandlingsresultat: Behandlingsresultat,
        opprinneligBehandlingsresultat: Behandlingsresultat
    ): Boolean {
        fun perioderITidligereÅr(br: Behandlingsresultat) = br.finnInnvilgedePerioderITidligereÅr()
            .map { periode ->
                AvgiftspliktigPeriodeTilSammenligning(
                    fom = periode.getFom(),
                    tom = periode.getTom()?.avkortTilForrigeÅr(),
                    trygdedekning = periode.hentTrygdedekning(),
                    erPliktigMedlemskap = periode.erPliktigMedlemskap()
                )
            }
            .toSet()

        return perioderITidligereÅr(behandlingsresultat) !=
                perioderITidligereÅr(opprinneligBehandlingsresultat)
    }

    private fun Behandlingsresultat.finnInnvilgedePerioderITidligereÅr(): List<AvgiftspliktigPeriode> {
        val currentYear = LocalDate.now().year
        return finnAvgiftspliktigPerioder()
            .filter { it.erInnvilget() }
            .filter { periode ->
                val fom = periode.getFom()
                fom != null && fom.year < currentYear
            }
    }

    private fun LocalDate.avkortTilForrigeÅr(): LocalDate {
        val forrigeÅr = LocalDate.now().year - 1
        return if (this.year > forrigeÅr) {
            LocalDate.of(forrigeÅr, 12, 31)
        } else {
            this
        }
    }
}

private data class AvgiftspliktigPeriodeTilSammenligning(
    val fom: LocalDate,
    val tom: LocalDate?,
    val trygdedekning: Trygdedekninger,
    val erPliktigMedlemskap: Boolean
)
