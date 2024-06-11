package no.nav.melosys.saksflyt.steg.arsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Component
class OpprettÅrsavregningBehandling(
    private val fagsakService: FagsakService,
    private val persondataService: PersondataService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val behandlingService: BehandlingService,
    private val lovvalgsperiodeService: LovvalgsperiodeService,
    private val medlemskapsperiodeService: MedlemskapsperiodeService,
    private val behandslingsresultatService: BehandlingsresultatService,
    private val oppretteÅrsavregning: ÅrsavregningService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_AARSAVREGNING_BEHANDLING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val gjelderÅr = prosessinstans.getData(ProsessDataKey.GJELDER_ÅR).toInt()
        val ident = prosessinstans.getData(ProsessDataKey.IDENTIFIKATOR)
        val aktørId = persondataService.hentAktørIdForIdent(ident)

        val sakMedTrygdeavgift = finnSakMedTrygdeavgift(aktørId).also {
            checkNotNull(it) { "Fant ingen sak med trygdeavgift for aktør: $aktørId" }
        } ?: return

        finnAktivÅrsavregningBehandling(sakMedTrygdeavgift, gjelderÅr)?.run {
            if (this.status != Behandlingsstatus.OPPRETTET) {
                status = Behandlingsstatus.VURDER_DOKUMENT
                behandlingService.lagre(this)
            }
            return
        }

        val trygdeavgiftsBehandlingMedRelevantPeriode = finnTrygdeavgiftsBehandlingMedRelevantPeriode(sakMedTrygdeavgift, gjelderÅr).also {
            if (it == null) log.info(
                "Fant ingen behandlinger med overlappende lovvalgsperioder eller medlemskapsperioder for sak: ${
                    sakMedTrygdeavgift.saksnummer
                } og år: $gjelderÅr"
            )
        } ?: return

        behandlingService.nyBehandling(
            sakMedTrygdeavgift,
            Behandlingsstatus.VURDER_DOKUMENT,
            Behandlingstyper.ÅRSAVREGNING,
            trygdeavgiftsBehandlingMedRelevantPeriode.tema,
            null,
            null,
            LocalDate.now(),
            Behandlingsaarsaktyper.MELDING_FRA_SKATT,
            null
        ).also { nyBehandling ->
            val behandlingsresultat = behandslingsresultatService.hentBehandlingsresultat(nyBehandling.id)
            oppretteÅrsavregning.oppretteÅrsavregning(behandlingsresultat, gjelderÅr)
            prosessinstans.behandling = nyBehandling
        }
    }

    private fun finnTrygdeavgiftsBehandlingMedRelevantPeriode(sakMedTrygdeavgift: Fagsak, gjelderPeriode: Int): Behandling? =
        trygdeavgiftService.hentTrygdeavgiftBehandlinger(sakMedTrygdeavgift.saksnummer).lastOrNull { behandling ->
            val lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandling.id)
            val medlemskapsperioder = medlemskapsperiodeService.hentMedlemskapsperioder(behandling.id)

            lovvalgsperioder.any { it.overlapperMedÅr(gjelderPeriode) } || medlemskapsperioder.any { it.overlapperMedÅr(gjelderPeriode) }
        }

    private fun finnAktivÅrsavregningBehandling(sakMedTrygdeavgift: Fagsak, gjelderPeriode: Int): Behandling? {
        val årsAvregninger = sakMedTrygdeavgift.hentAktiveÅrsavregninger().also { if (it.isEmpty()) log.info("Fant ingen aktive årsavregninger") }
            .filter { behandslingsresultatService.hentBehandlingsresultat(it.id).aarsavregning.aar == gjelderPeriode }

        when {
            årsAvregninger.isEmpty() -> {
                log.info("Fant ingen aktive årsavregninger for år $gjelderPeriode")
                return null
            }

            årsAvregninger.size > 1 -> {
                throw TekniskException("Flere aktive årsavregninger funnet")
            }

            else -> {
                log.info("Fant aktiv årsavregning for år $gjelderPeriode")
                return årsAvregninger.single()
            }
        }
    }

    private fun finnSakMedTrygdeavgift(aktørId: String): Fagsak? =
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId)
            .filter {
                trygdeavgiftService.harFagsakBehandlingerMedTrygdeavgift(it.saksnummer)
            }.let { sakerMedTrygdeavgift ->
                when {
                    sakerMedTrygdeavgift.isEmpty() -> null
                    sakerMedTrygdeavgift.size > 1 -> throw TekniskException("Flere saker med trygdeavgift funnet")
                    else -> sakerMedTrygdeavgift.single()
                }
            }
}
