package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.kodeverk.Betalingstype
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.integrasjon.dokgen.dto.Avgiftsperiode
import no.nav.melosys.integrasjon.dokgen.dto.AvgiftsperiodePensjonist
import no.nav.melosys.integrasjon.dokgen.dto.InformasjonTrygdeavgift
import no.nav.melosys.integrasjon.dokgen.dto.SvarAlternativ
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.helseutgiftdekkesperiode.GyldigeLand
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class InformasjonTrygdeavgiftMapper(
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService
    ) {

    @Transactional
    internal fun mapInformasjonTrygdeavgift(brevbestilling: DokgenBrevbestilling): InformasjonTrygdeavgift {
        val behandlingId = brevbestilling.behandling.id
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandlingId)
        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(behandlingId)
        val trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingId)

        return InformasjonTrygdeavgift(
            brevbestilling = brevbestilling,
            fomDato = helseutgiftDekkesPeriode.fomDato,
            tomDato = helseutgiftDekkesPeriode.tomDato,
            bostedLand = helseutgiftDekkesPeriode.bostedLandkode.beskrivelse,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            trygdeavgiftMottaker = trygdeavgiftMottaker,
            erNordisk = GyldigeLand.erNordiskLand(helseutgiftDekkesPeriode.bostedLandkode),
            betalingsvalg = hentBetalingsvalg(behandlingsresultat.behandling),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandlingsresultat.behandling),
            avgiftsperioder = mapAvgiftsperioderPensjonist(behandlingsresultat),
            innevaerendeAar = LocalDate.now().year,
            lavSatsInnevaerendeAar = 1.2,
            mellomSatsInnevaerendeAar = 2.1
        )
    }

    private fun mapAvgiftsperioderPensjonist(behandlingsresultat: Behandlingsresultat): List<Avgiftsperiode> {
        if (behandlingsresultat.trygdeavgiftsperioder.all {
                it.trygdeavgiftsbeløpMd.verdi == BigDecimal.ZERO && it.trygdesats == BigDecimal.ZERO
            }) {
            return emptyList()
        }

        return behandlingsresultat.trygdeavgiftsperioder.map {
            Avgiftsperiode(
                fom = it.periodeFra,
                tom = it.periodeTil,
                avgiftssats = it.trygdesats,
                avgiftPerMd = it.trygdeavgiftsbeløpMd.verdi,
                inntektskilde = it.grunnlagInntekstperiode!!.type.name,
                trygdedekning = it.grunnlagMedlemskapsperiodeNotNull.trygdedekning.beskrivelse,
                avgiftspliktigInntektPerMd = it.grunnlagInntekstperiode!!.avgiftspliktigMndInntekt?.verdi ?: BigDecimal.ZERO,
                arbeidsgiveravgiftBetalt = SvarAlternativ.IKKE_RELEVANT,
                skatteplikt = it.grunnlagSkatteforholdTilNorge!!.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG
            )
        }.sortedByDescending { it.fom }
    }


    private fun hentBetalingsvalg(behandling: Behandling): Betalingstype {
        return behandling.fagsak.betalingsvalg ?: Betalingstype.TREKK
    }

    private fun finnFullmektigTrygdeavgift(behandling: Behandling): String? {
        if (behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) == null) return null

        return trygdeavgiftsberegningService.finnFakturamottakerNavn(behandling.id)
    }
}
