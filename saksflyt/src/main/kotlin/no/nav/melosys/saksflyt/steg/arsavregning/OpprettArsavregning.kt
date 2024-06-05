package no.nav.melosys.saksflyt.steg.arsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
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

        val fagsaker = fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId)

        val sakMedTrygdeavgit = fagsaker.also { if (it.isEmpty()) log.info("Fant ingen fagsaker for aktør $aktørId") }
            .filter {
                // og der det har vært en overlapp i medlemskaps eller lovvalgsperioden for det året skatteoppgjør gjelder for
                trygdeavgiftOppsummeringService.harFagsakBehandlingerMedTrygdeavgift(it.saksnummer)
            }.let { sakerMedTrygdeavgit ->
                when {
                    sakerMedTrygdeavgit.isEmpty() -> {
                        log.info("Fant ingen saker med trygdeavgift saker: ${fagsaker.map { it.saksnummer }}")
                        return
                    }

                    sakerMedTrygdeavgit.size > 1 -> throw TekniskException("Flere saker med trygdeavgift funnet")
                    else -> sakerMedTrygdeavgit.single()
                }
            }

        val aktiveÅrsavregninger: List<Behandling> = sakMedTrygdeavgit.hentAktiveÅrsavregninger()

        val årsAvregninger = aktiveÅrsavregninger.also { if (it.isEmpty()) log.info("Fant ingen aktive årsavregninger") }
            .filter {
                behandslingsresultatService.hentBehandlingsresultat(it.id).aarsavregning.aar == gjelderPeriode && it.status != Behandlingsstatus.OPPRETTET
            }

        when {
            årsAvregninger.isEmpty() -> {
                log.info("Fant ingen aktive årsavregninger for år $gjelderPeriode")
            }

            årsAvregninger.size > 1 -> {
                throw TekniskException("Flere aktive årsavregninger funnet")
            }

            else -> {
                log.info("Fant aktiv årsavregning for år $gjelderPeriode")
                årsAvregninger.single().run {
                    status = Behandlingsstatus.VURDER_DOKUMENT
                    behandlingService.lagre(this)
                    return
                }
            }
        }

        val behandling = sakMedTrygdeavgit.hentSistRegistrertBehandling()
        val lovvalgsperioder: MutableCollection<Lovvalgsperiode> = lovvalgsperiodeService.hentLovvalgsperioder(behandling.id)
        val medlemskapsperioder = medlemskapsperiodeService.hentMedlemskapsperioder(behandling.id)

        val isWithinPeriod: (ErPeriode) -> Boolean = { it.fom.year <= gjelderPeriode && it.tom.year >= gjelderPeriode }

        if (lovvalgsperioder.none(isWithinPeriod) || medlemskapsperioder.none(isWithinPeriod)) {
            log.info("Fant ingen lovvalgsperioder eller medlemskapsperioder for behandling: ${behandling.id}")
            return
        }

        //og har vært fakturert trygdeavgift forskuddsvis for fra Melosys, for det året

        val nyBehandling = behandlingService.nyBehandling(
            sakMedTrygdeavgit,
            Behandlingsstatus.VURDER_DOKUMENT,
            Behandlingstyper.ÅRSAVREGNING,
            hentMedRiktigPeriode(sakMedTrygdeavgit),
            null,
            null,
            LocalDate.now(),
            Behandlingsaarsaktyper.ANNET, // Trenger vi en ny type?
            null
        )
        val behandlingsresultat = behandslingsresultatService.hentBehandlingsresultat(nyBehandling.id)
        oppretteÅrsavregning.oppretteÅrsavregning(behandlingsresultat, gjelderPeriode)

    }

    private fun hentMedRiktigPeriode(fagsak: Fagsak): Behandlingstema? {
        // TODO: Avklar om vi tenger å finne riktig periode
        return fagsak.hentSistRegistrertBehandling().tema
    }
}
