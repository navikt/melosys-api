package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.Unleash
import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
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
        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(behandlingId)

        if (helseutgiftDekkesPeriode == null) {
            throw IkkeFunnetException("Finner ingen helseutgift-periode med behandlingID: $behandlingId")
        }

        return InformasjonTrygdeavgift(
            brevbestilling = brevbestilling,
            fomDato = helseutgiftDekkesPeriode.fomDato,
            tomDato = helseutgiftDekkesPeriode.tomDato,
            bostedLand = helseutgiftDekkesPeriode.bostedLandkode.beskrivelse,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            trygdeavgiftMottaker = utledTrygdeavgiftsmottaker(behandlingsresultat),
            erNordisk = NordiskeLand.erNordiskLand(helseutgiftDekkesPeriode.bostedLandkode),
            betalingsvalg = hentBetalingsvalg(behandlingsresultat.hentBehandling()),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandlingsresultat.hentBehandling()),
            avgiftsperioder = mapAvgiftsperioderPensjonist(behandlingsresultat),
            harAvgiftspliktigePerioderIForegåendeÅr = if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
                behandlingsresultat.utledAvgiftspliktigperioderFom()?.let { fom ->
                    fom.year < LocalDate.now().year
                } ?: false
            } else {
                false
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

        if (perioder.all { it.trygdeavgiftsbeløpMd.verdi == BigDecimal.ZERO && it.trygdesats == BigDecimal.ZERO }) {
            return emptyList()
        }

        val inneværendeÅr = LocalDate.now().year
        val gruppertePerioder = perioder.groupBy { it.periodeTil.year }
        val valgtÅr = velgRelevantÅr(gruppertePerioder.keys, inneværendeÅr)

        return gruppertePerioder[valgtÅr]
            ?.map {
                AvgiftsperiodeEøsPensjonist(
                    fom = it.periodeFra,
                    tom = it.periodeTil,
                    avgiftssats = it.trygdesats,
                    avgiftPerMd = it.trygdeavgiftsbeløpMd.hentVerdi(),
                    inntektskilde = it.grunnlagInntekstperiode!!.type.beskrivelse,
                    avgiftspliktigInntektPerMd = it.grunnlagInntekstperiode!!.avgiftspliktigMndInntekt?.verdi ?: BigDecimal.ZERO,
                    skatteplikt = it.grunnlagSkatteforholdTilNorge!!.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG
                )
            }
            ?.sortedByDescending { it.fom }
            ?: emptyList()
    }

    private fun velgRelevantÅr(tilgjengeligeÅr: Set<Int>, inneværendeÅr: Int): Int {
        return when {
            inneværendeÅr in tilgjengeligeÅr -> inneværendeÅr
            tilgjengeligeÅr.all { it < inneværendeÅr } -> tilgjengeligeÅr.maxOrNull() ?: inneværendeÅr
            else -> tilgjengeligeÅr.minOrNull() ?: inneværendeÅr
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
