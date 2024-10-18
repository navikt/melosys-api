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
import no.nav.melosys.integrasjon.medl.MedlPeriodeKonverter.Companion.tilLovvalgBestemmelse
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.stream.Collectors

@Service
class LovvalgsperiodeService(
    private val behandlingsresultatRepo: BehandlingsresultatRepository,
    private val lovvalgsperiodeRepo: LovvalgsperiodeRepository,
    private val tidligereMedlemsperiodeRepository: TidligereMedlemsperiodeRepository,
    private val behandlingRepository: BehandlingRepository
) {
    fun hentLovvalgsperioder(behandlingsid: Long): Collection<Lovvalgsperiode> {
        return lovvalgsperiodeRepo.findByBehandlingsresultatId(behandlingsid)
    }

    fun hentLovvalgsperiode(behandlingsid: Long): Lovvalgsperiode {
        val lovvalgsperioder = hentLovvalgsperioder(behandlingsid)
        if (lovvalgsperioder.size != 1) {
            if (lovvalgsperioder.size > 1) {
                throw FunksjonellException(
                    "Fant %s lovvalgsperioder. Forventer kun én lovvalgsperiode"
                        .formatted(lovvalgsperioder.size)
                )
            } else {
                throw FunksjonellException("Fant ingen lovvalgsperiode. Forventer én lovvalgsperiode")
            }
        }
        val lovvalgsperiode = lovvalgsperioder.iterator().next()
        if (lovvalgsperiode.harUgyldigTilstand()) {
            throw FunksjonellException("Lovvalgsperioden har en ugyldig kombinasjon av resultat og lovvalgsland")
        }
        return lovvalgsperiode
    }

    @Transactional
    fun oppdaterLovvalgsperiode(lovvalgsperiodeId: Long, lovvalgsperiode: Lovvalgsperiode): Lovvalgsperiode {
        val lagretLovvalgsperiode = lovvalgsperiodeRepo.findById(lovvalgsperiodeId)
            .orElseThrow { FunksjonellException(String.format("Lovvalgsperioden %s finnes ikke", lovvalgsperiodeId)) }

        lagretLovvalgsperiode.fom = lovvalgsperiode.fom
        lagretLovvalgsperiode.tom = lovvalgsperiode.tom
        lagretLovvalgsperiode.lovvalgsland = lovvalgsperiode.lovvalgsland
        lagretLovvalgsperiode.bestemmelse = lovvalgsperiode.bestemmelse
        lagretLovvalgsperiode.tilleggsbestemmelse = lovvalgsperiode.tilleggsbestemmelse
        lagretLovvalgsperiode.innvilgelsesresultat = lovvalgsperiode.innvilgelsesresultat
        lagretLovvalgsperiode.dekning = lovvalgsperiode.dekning
        lagretLovvalgsperiode.medlemskapstype = lovvalgsperiode.medlemskapstype
        lagretLovvalgsperiode.medlPeriodeID = lovvalgsperiode.medlPeriodeID

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
            .orElseThrow { IllegalStateException(String.format("Behandling %s fins ikke.", behandlingsid)) }

        lovvalgsperiodeRepo.deleteByBehandlingsresultatId(behandlingsresultat.id)
        val lovvalgsperioderKopi = lovvalgsperioder.map { kopierLovvalgsperiodeMedBehandlingsResultat(it, behandlingsresultat) }

        return lovvalgsperiodeRepo.saveAllAndFlush(lovvalgsperioderKopi)
    }

    fun hentTidligereLovvalgsperioder(behandling: Behandling): Collection<Lovvalgsperiode> {
        val utvalgtePeriodeIDer = tidligereMedlemsperiodeRepository.findById_BehandlingId(behandling.id).stream()
            .map { utvalgtPeriode: TidligereMedlemsperiode -> utvalgtPeriode.id.periodeId }
            .collect(Collectors.toSet())

        if (utvalgtePeriodeIDer.isEmpty()) {
            return emptySet()
        }

        val medlemskapdokument = behandling.hentMedlemskapDokument()
        val perioder = medlemskapdokument.getMedlemsperiode().stream()
            .filter { periode: Medlemsperiode -> utvalgtePeriodeIDer.contains(periode.id) }
            .collect(Collectors.toSet())

        val tidligereLovvalgsperioder: MutableList<Lovvalgsperiode> = ArrayList()
        perioder.forEach {
            val lovvalgsperiode = Lovvalgsperiode().apply {
                fom = it.periode?.fom
                tom = it.periode?.tom
                medlPeriodeID = it.id
            }

            it.grunnlagstype?.let { grunnlagsType ->
                val isValidEnum = GrunnlagMedl.values().any { gm -> gm.name == grunnlagsType }
                if (isValidEnum) {
                    val grunnlagMedlKode = GrunnlagMedl.valueOf(grunnlagsType)
                    lovvalgsperiode.bestemmelse = tilLovvalgBestemmelse(grunnlagMedlKode)
                } else {
                    lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET
                }

            }
            tidligereLovvalgsperioder.add(lovvalgsperiode)
        }
        return tidligereLovvalgsperioder
    }

    fun hentOpprinneligLovvalgsperiode(behandlingId: Long): Lovvalgsperiode {
        val behandling = behandlingRepository.findById(behandlingId)
            .orElseThrow { IkkeFunnetException("Fant ingen behandling for $behandlingId") }

        val opprinneligBehandling = Optional.ofNullable(behandling.opprinneligBehandling)
            .orElseThrow { IkkeFunnetException("Fant ingen opprinnelig behandling for $behandlingId") }

        val lovvalgsperiodeList = lovvalgsperiodeRepo.findByBehandlingsresultatId(opprinneligBehandling.id)
        return lovvalgsperiodeList.stream()
            .findFirst()
            .orElseThrow { IkkeFunnetException("Fant ingen opprinnelig lovvalgsperiode for $behandlingId") }
    }

    fun finnOpprinneligLovvalgsperiode(behandlingId: Long): Optional<Lovvalgsperiode> {
        return behandlingRepository.findById(behandlingId).map { obj: Behandling -> obj.opprinneligBehandling }
            .flatMap { behandling: Behandling ->
                lovvalgsperiodeRepo.findByBehandlingsresultatId(behandling.id).stream().findFirst()
            }
    }

    fun harSelvstendigNæringsdrivendeLovvalgsbestemmelse(behandlingId: Long): Boolean {
        val lovvalgBestemmelse = hentLovvalgsperiode(behandlingId).bestemmelse
        return lovvalgBestemmelse == Lovvalgsbestemmelser_trygdeavtale_ca.CAN_ART6_2 || lovvalgBestemmelse == Lovvalgsbestemmelser_trygdeavtale_us.USA_ART5_4
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
