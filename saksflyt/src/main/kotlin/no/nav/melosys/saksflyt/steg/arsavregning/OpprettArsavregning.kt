package no.nav.melosys.saksflyt.steg.arsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.ErPeriode
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
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.TrygdeavgiftOppsummeringService
import no.nav.melosys.service.sak.ÅrsavregningService
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Component
class OpprettArsavregning(
    private val fagsakService: FagsakService,
    private val persondataService: PersondataService,
    private val trygdeavgiftOppsummeringService: TrygdeavgiftOppsummeringService,
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
        val gjelderPeriode = prosessinstans.getData(ProsessDataKey.GJELDER_PERIODE).toInt()
        val ident = prosessinstans.getData(ProsessDataKey.IDENTIFIKATOR)
        val aktørId = persondataService.hentAktørIdForIdent(ident)

        val sakMedTrygdeavgift = finnSakMedTrygdeavgift(aktørId).also {
            if (it == null) log.info("Fant ingen sak med trygdeavgift for prossesInstansId: ${prosessinstans.id}")
        } ?: return

        finnAktivÅrsavregning(sakMedTrygdeavgift, gjelderPeriode)?.run {
            if (this.status != Behandlingsstatus.OPPRETTET) {
                status = Behandlingsstatus.VURDER_DOKUMENT
                behandlingService.lagre(this)
            }
            return
        }

        val trygdeavgiftsBehandlingerMedGyldigPeriode = finnTrygdeavgiftsBehandlingerMedGyldigPeriode(sakMedTrygdeavgift, gjelderPeriode).also {
            if (it == null) log.info(
                "Fant ingen behandlinger med overlappende lovvalgsperioder eller medlemskapsperioder for sak: ${
                    sakMedTrygdeavgift.saksnummer
                } og år: $gjelderPeriode"
            )
        } ?: return

        val nyBehandling = behandlingService.nyBehandling(
            sakMedTrygdeavgift,
            Behandlingsstatus.VURDER_DOKUMENT,
            Behandlingstyper.ÅRSAVREGNING,
            trygdeavgiftsBehandlingerMedGyldigPeriode.tema,
            null,
            null,
            LocalDate.now(),
            Behandlingsaarsaktyper.ANNET, // Trenger vi en ny type?
            null
        )
        val behandlingsresultat = behandslingsresultatService.hentBehandlingsresultat(nyBehandling.id)
        oppretteÅrsavregning.oppretteÅrsavregning(behandlingsresultat, gjelderPeriode)
        prosessinstans.behandling = nyBehandling
    }

    private fun finnTrygdeavgiftsBehandlingerMedGyldigPeriode(sakMedTrygdeavgift: Fagsak, gjelderPeriode: Int): Behandling? {
        return trygdeavgiftOppsummeringService.hentTrygdeavgiftBehandlinger(sakMedTrygdeavgift.saksnummer).firstOrNull {
            val lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(it.id)
            val medlemskapsperioder = medlemskapsperiodeService.hentMedlemskapsperioder(it.id)

            val isWithinPeriod: (ErPeriode) -> Boolean = { it.fom.year <= gjelderPeriode && it.tom.year >= gjelderPeriode }
            lovvalgsperioder.none(isWithinPeriod) || medlemskapsperioder.none(isWithinPeriod)
        }
    }

    private fun finnAktivÅrsavregning(sakMedTrygdeavgift: Fagsak, gjelderPeriode: Int): Behandling? {
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
                trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(it.saksnummer)
            }.let { sakerMedTrygdeavgift ->
                when {
                    sakerMedTrygdeavgift.isEmpty() -> null
                    sakerMedTrygdeavgift.size > 1 -> throw TekniskException("Flere saker med trygdeavgift funnet")
                    else -> sakerMedTrygdeavgift.single()
                }
            }
}
