package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.Unleash
import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.Betalingstype
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.dokgen.dto.AvgiftsperiodeEøsPensjonist
import no.nav.melosys.integrasjon.dokgen.dto.InformasjonTrygdeavgift
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.helseutgiftdekkesperiode.NordiskeLand
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class InformasjonTrygdeavgiftMapper(
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val unleash: Unleash
) {

    @Transactional
    internal fun mapInformasjonTrygdeavgift(brevbestilling: DokgenBrevbestilling): InformasjonTrygdeavgift {
        val behandlingId = brevbestilling.behandlingNonNull().id
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandlingId)
        val helseutgiftDekkesPerioder = helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPerioder(behandlingId)

        if (helseutgiftDekkesPerioder.isEmpty()) {
            throw IkkeFunnetException("Finner ingen helseutgift-perioder med behandlingID: $behandlingId")
        }

        val distinctLandkoder = helseutgiftDekkesPerioder.map { it.bostedLandkode }.distinct()
        if (distinctLandkoder.size > 1) {
            throw IllegalStateException(
                "Forventer at alle helseutgift-perioder har samme landkode, men fant: ${distinctLandkoder.map { it.kode }}"
            )
        }

        val fomDato = helseutgiftDekkesPerioder.minOf { it.fomDato }
        val tomDato = helseutgiftDekkesPerioder.maxOf { it.tomDato }
        val førstePeriode = helseutgiftDekkesPerioder.first()

        return InformasjonTrygdeavgift(
            brevbestilling = brevbestilling,
            fomDato = fomDato,
            tomDato = tomDato,
            bostedLand = førstePeriode.bostedLandkode.beskrivelse,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            trygdeavgiftMottaker = utledTrygdeavgiftsmottaker(behandlingsresultat),
            erNordisk = NordiskeLand.erNordiskLand(førstePeriode.bostedLandkode),
            betalingsvalg = hentBetalingsvalg(behandlingsresultat.hentBehandling()),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandlingsresultat.hentBehandling()),
            avgiftsperioder = mapAvgiftsperioderPensjonist(behandlingsresultat),
            harAvgiftspliktigePerioderIForegåendeÅr = if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
                behandlingsresultat.utledAvgiftspliktigperioderFom()?.let { fom ->
                    fom.year < LocalDate.now().year
                } ?: false
            } else {
                false
            },
            erSkattemessigEmigrert = behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.any {
                it.grunnlagSkatteforholdTilNorge?.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )
    }

    private fun utledTrygdeavgiftsmottaker(behandlingsresultat: Behandlingsresultat): Trygdeavgiftmottaker? {
        if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER) && behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.isEmpty()) {
            return null
        }
        return trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.toList())
    }

    private fun mapAvgiftsperioderPensjonist(behandlingsresultat: Behandlingsresultat): List<AvgiftsperiodeEøsPensjonist> {
        val perioder = behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.toSet()

        if (perioder.all { !it.harAvgift() && it.beregningsregel == Avgiftsberegningsregel.ORDINÆR }) {
            return emptyList()
        }

        val inneværendeÅr = LocalDate.now().year
        val gruppertePerioder = perioder.groupBy { it.periodeTil.year }
        val årMedAvgift = gruppertePerioder.filterValues { årsperioder ->
            årsperioder.any { it.harAvgift() }
        }.keys
        val valgtÅr = velgRelevantÅr(årMedAvgift, inneværendeÅr)
            ?: return emptyList()

        return gruppertePerioder[valgtÅr]
            ?.map {
                val inntektsperiode = it.hentGrunnlagInntekstperiode()
                AvgiftsperiodeEøsPensjonist(
                    fom = it.periodeFra,
                    tom = it.periodeTil,
                    avgiftssats = it.trygdesats,
                    avgiftPerMd = it.trygdeavgiftsbeløpMd.hentVerdi(),
                    inntektskilde = inntektsperiode.type.beskrivelse,
                    avgiftspliktigInntektPerMd = inntektsperiode.avgiftspliktigMndInntekt?.verdi ?: BigDecimal.ZERO,
                    skatteplikt = it.hentGrunnlagSkatteforholdTilNorge().skatteplikttype == Skatteplikttype.SKATTEPLIKTIG,
                    beregningsregel = it.beregningsregel.takeIf { regel -> regel != Avgiftsberegningsregel.ORDINÆR }?.name,
                    minstebelopVerdi = it.minstebelopVerdi,
                    minstebelopAar = it.minstebelopAar,
                )
            }
            ?.sortedByDescending { it.fom }
            ?: emptyList()
    }

    private fun velgRelevantÅr(tilgjengeligeÅr: Set<Int>, inneværendeÅr: Int): Int? {
        if (tilgjengeligeÅr.isEmpty()) return null
        return when {
            inneværendeÅr in tilgjengeligeÅr -> inneværendeÅr
            tilgjengeligeÅr.all { it < inneværendeÅr } -> tilgjengeligeÅr.maxOrNull()
            else -> tilgjengeligeÅr.minOrNull()
        }
    }

    private fun hentBetalingsvalg(behandling: Behandling): Betalingstype {
        return behandling.fagsak.betalingsvalg ?: Betalingstype.TREKK
    }

    private fun finnFullmektigTrygdeavgift(behandling: Behandling): String? {
        if (behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) == null) return null

        return trygdeavgiftsberegningService.finnFakturamottakerNavn(behandling.id)
    }
}
