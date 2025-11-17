package no.nav.melosys.service

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.TidligereMedlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_ca
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_us
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.medl.GrunnlagMedl
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.tilLovvalgBestemmelse
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
class LovvalgsperiodeService(
    private val behandlingsresultatRepo: BehandlingsresultatRepository,
    private val lovvalgsperiodeRepo: LovvalgsperiodeRepository,
    private val tidligereMedlemsperiodeRepository: TidligereMedlemsperiodeRepository,
    private val behandlingRepository: BehandlingRepository
) {
    fun hentLovvalgsperioder(behandlingsid: Long): Collection<Lovvalgsperiode> =
        lovvalgsperiodeRepo.findByBehandlingsresultatId(behandlingsid)

    fun hentLovvalgsperiode(behandlingsid: Long): Lovvalgsperiode = hentLovvalgsperioder(behandlingsid).let { perioder ->
        perioder.singleOrNull()
            ?.takeUnless { it.harUgyldigTilstand() }
            ?: throw FunksjonellException(
                when {
                    perioder.isEmpty() -> "Fant ikke lovvalgsperioder"
                    perioder.size > 1 -> "Fant flere enn en lovvalgsperiode, forventer kun 1"
                    else -> "Lovvalgsperioden har en ugyldig kombinasjon av resultat og lovvalgsland"
                }
            )
    }

    @Transactional
    fun oppdaterLovvalgsperiode(lovvalgsperiodeId: Long, lovvalgsperiode: Lovvalgsperiode): Lovvalgsperiode {
        val lagretLovvalgsperiode = lovvalgsperiodeRepo.findById(lovvalgsperiodeId)
            .orElseThrow { FunksjonellException("Lovvalgsperiode med id $lovvalgsperiodeId finnes ikke") }

        lagretLovvalgsperiode.apply {
            fom = lovvalgsperiode.fom
            tom = lovvalgsperiode.tom
            lovvalgsland = lovvalgsperiode.lovvalgsland
            bestemmelse = lovvalgsperiode.bestemmelse
            tilleggsbestemmelse = lovvalgsperiode.tilleggsbestemmelse
            innvilgelsesresultat = lovvalgsperiode.innvilgelsesresultat
            dekning = lovvalgsperiode.dekning
            medlemskapstype = lovvalgsperiode.medlemskapstype
            medlPeriodeID = lovvalgsperiode.medlPeriodeID
        }

        return lovvalgsperiodeRepo.save(lagretLovvalgsperiode)
    }

    @Transactional
    fun slettLovvalgsperiode(lovvalgsperiodeId: Long) {
        lovvalgsperiodeRepo.deleteById(lovvalgsperiodeId)
    }

    @Transactional
    fun lagreLovvalgsperioder(
        behandlingsid: Long,
        lovvalgsperioder: Collection<Lovvalgsperiode>
    ): Collection<Lovvalgsperiode> {
        val behandlingsresultat = behandlingsresultatRepo.findById(behandlingsid)
            .orElseThrow { IllegalStateException("Behandling med id $behandlingsid fins ikke.") }

        val eksisterende = lovvalgsperiodeRepo.findByBehandlingsresultatId(behandlingsresultat.hentId())
        if (eksisterende.isNotEmpty()) {
            // 1: Fjern trygdeavgiftsperioder og persister endringen
            eksisterende.forEach { periode ->
                periode.clearTrygdeavgiftsperioder()
                lovvalgsperiodeRepo.save(periode) 
            }
            lovvalgsperiodeRepo.flush()

            // 2: Slett selve periodene etter at children er slettet
            lovvalgsperiodeRepo.deleteAllInBatch(eksisterende)
            lovvalgsperiodeRepo.flush()
        }

        val lovvalgsperioderKopi = lovvalgsperioder.map { kopierLovvalgsperiodeMedBehandlingsResultat(it, behandlingsresultat) }

        return lovvalgsperiodeRepo.saveAllAndFlush(lovvalgsperioderKopi)
    }

    fun hentTidligereLovvalgsperioder(behandling: Behandling): Collection<Lovvalgsperiode> {
        val utvalgtePeriodeIDer = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandling.id)
            .map { utvalgtPeriode: TidligereMedlemsperiode -> utvalgtPeriode.id.periodeId }
            .toSet()

        if (utvalgtePeriodeIDer.isEmpty()) {
            return emptySet()
        }

        val perioder = behandling.hentMedlemskapDokument()
            .medlemsperiode
            .filter { periode: Medlemsperiode -> utvalgtePeriodeIDer.contains(periode.id) }
            .toSet()

        return perioder.map { periode ->
            Lovvalgsperiode().apply {
                fom = periode.periode?.fom
                tom = periode.periode?.tom
                medlPeriodeID = periode.id

                periode.grunnlagstype?.let { grunnlagsType ->
                    bestemmelse = GrunnlagMedl.entries.find { it.name == grunnlagsType }
                        ?.let { tilLovvalgBestemmelse(it) }
                        ?: Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET
                }
            }
        }
    }

    fun hentOpprinneligLovvalgsperiode(behandlingId: Long): Lovvalgsperiode {
        val behandling = behandlingRepository.findById(behandlingId)
            .orElseThrow { IkkeFunnetException("Fant ingen behandling for $behandlingId") }

        val opprinneligBehandling = behandling.opprinneligBehandling
            ?: throw IkkeFunnetException("Fant ingen opprinnelig behandling for $behandlingId")

        val lovvalgsperiodeList = lovvalgsperiodeRepo.findByBehandlingsresultatId(opprinneligBehandling.id)

        return lovvalgsperiodeList
            .firstOrNull() ?: throw IkkeFunnetException("Fant ingen opprinnelig lovvalgsperiode for $behandlingId")
    }

    fun finnOpprinneligLovvalgsperiode(behandlingId: Long): Lovvalgsperiode? =
        behandlingRepository.findById(behandlingId).getOrNull()?.opprinneligBehandling?.let {
            lovvalgsperiodeRepo.findByBehandlingsresultatId(it.id).firstOrNull()
        }

    fun harSelvstendigNæringsdrivendeLovvalgsbestemmelse(behandlingId: Long): Boolean =
        when (hentLovvalgsperiode(behandlingId).bestemmelse) {
            Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2,
            Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4 -> true

            else -> false
        }

    private fun kopierLovvalgsperiodeMedBehandlingsResultat(
        lovvalgsperiode: Lovvalgsperiode,
        behandlingsresultat: Behandlingsresultat
    ): Lovvalgsperiode {
        val kopi = BeanUtils.cloneBean(lovvalgsperiode) as Lovvalgsperiode
        kopi.behandlingsresultat = behandlingsresultat
        return kopi
    }
}
