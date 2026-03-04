package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
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
            val opprinneligBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)

            val årMedEndringer = finnÅrMedEndringer(behandlingsresultat, opprinneligBehandlingsresultat)
            if (årMedEndringer.isNotEmpty()) {
                opprettÅrsavregning(årMedEndringer, behandling)
            }
        }
    }

    private fun finnÅrMedEndringer(
        behandlingsresultat: Behandlingsresultat,
        opprinneligBehandlingsresultat: Behandlingsresultat
    ): Set<Int> {
        val potensielleÅr = hentPotensielleÅrsavregningÅrFraAvgiftsperioder(opprinneligBehandlingsresultat)
            .union(hentPotensielleÅrsavregningÅrFraAvgiftsperioder(behandlingsresultat))

        return potensielleÅr.filter { år ->
            perioderForÅr(behandlingsresultat, år) != perioderForÅr(opprinneligBehandlingsresultat, år)
        }.toSet()
    }

    private fun perioderForÅr(
        behandlingsresultat: Behandlingsresultat,
        år: Int
    ): Set<AvgiftspliktigPeriodeTilSammenligning> {
        return behandlingsresultat.finnAvgiftspliktigPerioder()
            .filter { it.erInnvilget() }
            .filter { periode ->
                val fom = periode.getFom() ?: return@filter false
                val tom = periode.getTom()
                val sluttÅr = tom?.year ?: LocalDate.now().year
                år in fom.year..sluttÅr
            }
            .map { periode ->
                AvgiftspliktigPeriodeTilSammenligning(
                    fom = maxOf(periode.getFom(), LocalDate.of(år, 1, 1)),
                    tom = periode.getTom()?.let { minOf(it, LocalDate.of(år, 12, 31)) }
                        ?: LocalDate.of(år, 12, 31),
                    trygdedekning = periode.hentTrygdedekning(),
                    erPliktigMedlemskap = periode.erPliktigMedlemskap()
                )
            }
            .toSet()
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
}

private data class AvgiftspliktigPeriodeTilSammenligning(
    val fom: LocalDate,
    val tom: LocalDate,
    val trygdedekning: Trygdedekninger,
    val erPliktigMedlemskap: Boolean
)
