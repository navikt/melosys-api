package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ÅrsavregningService(
    private val aarsavregningRepository: AarsavregningRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val trygdeavgiftService: TrygdeavgiftService,
) {

    @Transactional(readOnly = true)
    fun finnÅrsavregning(behandlingID: Long): ÅrsavregningModel? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        val aarsavregning = behandlingsresultat.årsavregning ?: return null

        return lagÅrsavregningModelFraÅrsavregning(aarsavregning)
    }

    @Transactional
    fun opprettÅrsavregning(behandlingID: Long, gjelderÅr: Int): ÅrsavregningModel {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        if (behandlingsresultat.årsavregning != null && behandlingsresultat.årsavregning?.aar == gjelderÅr) {
            throw FunksjonellException("Året $gjelderÅr er allerede lagret på denne årsavregningen")
        }
        if (aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(behandlingID, gjelderÅr) != 0) {
            throw FunksjonellException(
                "Det finnes en annen åpen årsavregningsbehandling for samme år på saken. " +
                    "Vurder hvilke behandling du vil fortsette med og avslutt den uaktuelle behandlingen via behandlingsmeny."
            )
        }

        if (gjelderÅr < LocalDate.now().year - antall_år_tilbake_i_tid) {
            throw FunksjonellException("Årsavregning kan ikke opprettes for år eldre enn 6 år før inneværende år.")
        }

        if (behandlingsresultat.årsavregning != null) {
            behandlingsresultat.årsavregning?.behandlingsresultat = null
            behandlingsresultat.årsavregning = null;
            behandlingsresultatService.lagreOgFlush(behandlingsresultat)
        }

        val tidligereBehandlingsresultatMedAvgift = finnTidligereBehandlingsresultatMedAvgift(behandlingsresultat, gjelderÅr)
        replikerMedlemskapsperioder(behandlingsresultat, tidligereBehandlingsresultatMedAvgift, gjelderÅr)

        val årsavregning = Årsavregning().apply {
            behandlingsresultat.årsavregning = this
            aar = gjelderÅr
            this.behandlingsresultat = behandlingsresultat
            tidligereBehandlingsresultat = tidligereBehandlingsresultatMedAvgift
            tidligereFakturertBeloep = hentTotalAvgift(tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(gjelderÅr) }.orEmpty())
        }.also {
            behandlingsresultatService.lagre(behandlingsresultat)
        }

        return lagÅrsavregningModelFraÅrsavregning(årsavregning)
    }

    public fun hentTotalAvgift(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal? {
        if(trygdeavgiftsperioder.isEmpty()){
            return null
        }
        val fakturaseriePerioder = trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                startDato = it.periodeFra,
                sluttDato = it.periodeTil,
                enhetsprisPerManed = it.trygdeavgiftsbeløpMd.verdi,
                beskrivelse = "FIXME"
            )
        }
        val saksbehandlerIdent = SubjectHandler.getInstance().getUserID()
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder), saksbehandlerIdent)
    }

    public fun hentTotalInntekt(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal {
        val fakturaseriePerioder = trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                startDato = it.periodeFra,
                sluttDato = it.periodeTil,
                enhetsprisPerManed = it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi,
                beskrivelse = "FIXME"
            )
        }
        val saksbehandlerIdent = SubjectHandler.getInstance().getUserID()
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder), saksbehandlerIdent)
    }

    private fun replikerMedlemskapsperioder(behandlingsresultat: Behandlingsresultat, tidligereBehandlingsresultat: Behandlingsresultat?, gjelderÅr: Int) {
        behandlingsresultat.medlemskapsperioder.clear()
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
        if (tidligereBehandlingsresultat != null) {
            for (medlemskapsperiodeOriginal in tidligereBehandlingsresultat.medlemskapsperioder) {
                if(medlemskapsperiodeOriginal.overlapperMedÅr(gjelderÅr)) {
                    val medlemskapsperiodeReplika = BeanUtils.cloneBean(medlemskapsperiodeOriginal) as Medlemskapsperiode
                    medlemskapsperiodeReplika.behandlingsresultat = behandlingsresultat
                    medlemskapsperiodeReplika.trygdeavgiftsperioder = HashSet()
                    medlemskapsperiodeReplika.avkortFomDato(gjelderÅr)
                    medlemskapsperiodeReplika.id = null
                    behandlingsresultat.addMedlemskapsperiode(medlemskapsperiodeReplika)
                }
            }
        }
    }

    // TODO [MELOSYS-6757] mangler støtte for årsavregning uten tidligere behandling
    private fun lagÅrsavregningModelFraÅrsavregning(årsavregning: Årsavregning): ÅrsavregningModel {
        val år = årsavregning.aar

        return ÅrsavregningModel(
            år = år,
            tidligereGrunnlag = hentTidligereTrygdeavgiftsgrunnlag(år, årsavregning.tidligereBehandlingsresultat),
            tidligereAvgift = årsavregning.tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(år) }.orEmpty(),
            nyttGrunnlag = hentNyttTrygdeavgiftsgrunnlag(årsavregning),
            endeligAvgift = årsavregning.behandlingsresultat.trygdeavgiftsperioder.toList(),
            tidligereFakturertBeloep = årsavregning.tidligereFakturertBeloep,
            nyttTotalbeloep = årsavregning.nyttTotalbeloep,
            tilFaktureringBeloep = årsavregning.tilFaktureringBeloep
        )
    }

    private fun finnTidligereBehandlingsresultatMedAvgift(behandlingsresultat: Behandlingsresultat, gjelderÅr: Int): Behandlingsresultat? {
        val saksnummer = behandlingsresultat.behandling.fagsak.saksnummer
        val behandling = trygdeavgiftService.finnSistFakturerbarTrygdeavgiftsbehandlingForÅr(saksnummer, gjelderÅr) ?: return null
        return if (behandlingsresultat.behandling == behandling) {
            null
        } else
            behandlingsresultatService.hentBehandlingsresultat(behandling.id)
    }

    private fun hentTidligereTrygdeavgiftsgrunnlag(år: Int, behandlingsresultat: Behandlingsresultat?): Trygdeavgiftsgrunnlag? {
        if (behandlingsresultat == null) return null

        return Trygdeavgiftsgrunnlag(
            medlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.overlapperMedÅr(år) }.map(::MedlemskapsperiodeForAvgift),
            skatteforholdsperioder = behandlingsresultat.hentSkatteforholdTilNorge().filter { it.overlapperMedÅr(år) },
            innteksperioder = behandlingsresultat.hentInntektsperioder().filter { it.overlapperMedÅr(år) }
        )
    }

    private fun hentNyttTrygdeavgiftsgrunnlag(årsavregning: Årsavregning): Trygdeavgiftsgrunnlag? {
        val behandlingsresultat = årsavregning.behandlingsresultat
        if (behandlingsresultat.medlemskapsperioder.isEmpty() && behandlingsresultat.hentSkatteforholdTilNorge()
                .isEmpty() && behandlingsresultat.hentInntektsperioder().isEmpty()
        ) {
            return null
        }
        return Trygdeavgiftsgrunnlag(
            medlemskapsperioder = behandlingsresultat.medlemskapsperioder.map(::MedlemskapsperiodeForAvgift),
            skatteforholdsperioder = behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            innteksperioder = behandlingsresultat.hentInntektsperioder().toList()
        )
    }

    companion object {
        private val antall_år_tilbake_i_tid = 7  //Fjoråret - 6 år
    }
}

data class ÅrsavregningModel(
    val år: Int,
    val tidligereGrunnlag: Trygdeavgiftsgrunnlag?,
    val tidligereAvgift: List<Trygdeavgiftsperiode>,
    val nyttGrunnlag: Trygdeavgiftsgrunnlag?,
    val endeligAvgift: List<Trygdeavgiftsperiode>,
    val tidligereFakturertBeloep: BigDecimal?,
    val nyttTotalbeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
)

data class Trygdeavgiftsgrunnlag(
    val medlemskapsperioder: List<MedlemskapsperiodeForAvgift>,
    val skatteforholdsperioder: List<SkatteforholdTilNorge>,
    val innteksperioder: List<Inntektsperiode>
)

data class MedlemskapsperiodeForAvgift(
    val fom: LocalDate, val tom: LocalDate, val dekning: Trygdedekninger, val bestemmelse: Folketrygdloven_kap2_bestemmelser, val medlemskapstyper: Medlemskapstyper
) {
    constructor(medlemskapsperiode: Medlemskapsperiode) : this(
        fom = medlemskapsperiode.fom,
        tom = medlemskapsperiode.tom,
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype
    )
}
