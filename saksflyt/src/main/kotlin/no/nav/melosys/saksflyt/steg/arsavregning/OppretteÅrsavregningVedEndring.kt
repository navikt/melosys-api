package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
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

    private fun erArtikkel11_3B(behandlingsresultat: Behandlingsresultat): Boolean {
        return behandlingsresultat.lovvalgsperioder.any {
            it.getBestemmelse() == Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3B
        }
    }

    // Er egne oppgaver som skal legge til FTRL.penjonist, EØS.pensjonist|offentlig-tjenesteperson år årsavregning er ok
    fun harTemaOgTypeSomSkalBehandles(behandling: Behandling, fagsak: Fagsak, behandlingsresultat: Behandlingsresultat) : Boolean {
        val ftrlYrkesaktiv = behandling.tema == Behandlingstema.YRKESAKTIV && fagsak.type == Sakstyper.FTRL
        val ftrlPensjonist = behandling.tema == Behandlingstema.PENSJONIST && fagsak.type == Sakstyper.FTRL
        val eøsOffentligtjenesteperson = behandling.tema == Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
            && fagsak.type == Sakstyper.EU_EOS
            && erArtikkel11_3B(behandlingsresultat)
        val eøsTrygdeavgiftpensjonist = behandling.tema == Behandlingstema.PENSJONIST && fagsak.type == Sakstyper.EU_EOS && fagsak.tema == Sakstemaer.TRYGDEAVGIFT

        return ftrlYrkesaktiv || eøsOffentligtjenesteperson || eøsTrygdeavgiftpensjonist || ftrlPensjonist
    }

    /**
     * MELOSYS-8148: innhentingsbrev skal sendes automatisk for sakstypene i scope (FTRL yrkesaktiv,
     * FTRL pensjonist og EØS offentlig tjenesteperson), men IKKE for EØS-pensjonist (trygdeavgift) —
     * de skal ha egen brevtekst og dekkes av en senere oppgave. Kalles kun etter at
     * [harTemaOgTypeSomSkalBehandles] har returnert true, så det eneste tilfellet som skal unntas er
     * EØS-pensjonist.
     */
    private fun skalSendeInnhentingsbrev(behandling: Behandling, fagsak: Fagsak): Boolean {
        val erEøsPensjonist = behandling.tema == Behandlingstema.PENSJONIST
            && fagsak.type == Sakstyper.EU_EOS
            && fagsak.tema == Sakstemaer.TRYGDEAVGIFT
        return !erEøsPensjonist
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            return
        }

        val behandling = prosessinstans.hentBehandling
        val fagsak = behandling.fagsak
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)


        if (!harTemaOgTypeSomSkalBehandles(behandling, fagsak, behandlingsresultat)) {
            return
        }

        // MELOSYS-8148: når årsavregningen opprettes automatisk i saksbehandlingsflyten skal brevet
        // «Innhenting av inntektsopplysninger» sendes automatisk (samme steg som MELOSYS-8122 — gates av
        // prosessdata-flagget SEND_INNHENTINGSBREV). EØS-pensjonist er eksplisitt unntatt; de får egen
        // brevtekst i en senere oppgave.
        val sendInnhentingsbrev = skalSendeInnhentingsbrev(behandling, fagsak)

        val potensielleÅrsavregningÅrNy: Set<Int> = hentPotensielleÅrsavregningÅrFraAvgiftsperioder(behandlingsresultat)

        if (behandling.erFørstegangsvurdering()) {
            opprettÅrsavregning(potensielleÅrsavregningÅrNy, behandling, sendInnhentingsbrev)
        } else if (behandling.erNyVurdering()) {
            val opprinneligBehandling = behandling.hentOpprinneligBehandling()
            val opprinneligBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)

            val årMedEndringer = finnÅrMedEndringer(behandlingsresultat, opprinneligBehandlingsresultat)
            if (årMedEndringer.isNotEmpty()) {
                opprettÅrsavregning(årMedEndringer, behandling, sendInnhentingsbrev)
            }
        } else {
            log.debug("Behandling ${behandling.id} er verken førstegang eller ny vurdering, oppretter ikke årsavregning")
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
        behandling: Behandling,
        sendInnhentingsbrev: Boolean
    ) {
        potensielleÅrsavregningÅr.forEach { potensieltÅr ->
            val harAktivÅrsavregningforÅr =
                årsavregningService.harAktivÅrsavregningForÅr(behandling.fagsak.saksnummer, potensieltÅr)
            if (!harAktivÅrsavregningforÅr) {
                log.info("Oppretter årsavregningsbehandling for år $potensieltÅr på saksnummer ${behandling.fagsak.saksnummer} basert på behandling ${behandling.id}")
                prosessInstansService.opprettArsavregningsBehandlingProsessflyt(
                    behandling.fagsak.saksnummer,
                    potensieltÅr.toString(),
                    Behandlingsaarsaktyper.AUTOMATISK_OPPRETTELSE,
                    sendInnhentingsbrev
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
