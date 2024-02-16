package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.brev.InnvilgelseFtrlBrevbestilling
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Kodeverk
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftsperiodeDto
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.transaction.Transactional

@Component
class InnvilgelseFtrlMapper(
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService
) {
    @Transactional
    fun map(brevbestilling: InnvilgelseFtrlBrevbestilling): InnvilgelseFtrl {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val arbeidsland =
            behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland.landkoder[0]

        return InnvilgelseFtrl.Builder(brevbestilling)
            .behandlingstype(behandlingsresultat.behandling.type)
            .avgiftsperioder(mapAvgiftsPerioder(medlemAvFolketrygden))
            .medlemskapsperioder(mapMedlemskapsPerioder(medlemAvFolketrygden))
            .bestemmelse(medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget() }.sortedBy { it.fom }.first().bestemmelse)
            .avslåttMedlemskapsperiodeFørMottaksdatoHelsedel(
                medlemAvFolketrygden.medlemskapsperioder.any {
                    it.erAvslaatt() && it.harHelsedelDekning() && it.fomErFør(brevbestilling.forsendelseMottatt)
                }
            )
            .avslåttMedlemskapsperiodeFørMottaksdatoFullDekning(
                medlemAvFolketrygden.medlemskapsperioder.any {
                    it.erAvslaatt() && it.harFullDekning() && it.fomErFør(brevbestilling.forsendelseMottatt)
                }
            )
            .trygdeavgiftMottaker(trygdeavgiftMottakerService.getTrygdeavgiftMottaker(medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag))
            .fullmektigTrygdeavgift(finnFullmektigTrygdeavgift(behandlingsresultat.behandling))
            .skatteplikttype(medlemAvFolketrygden.utledSkatteplikttype())
            .begrunnelse(hentBegrunnelse(behandlingsresultat.vilkaarsresultater))
            .begrunnelseAnnenGrunnFritekst(hentSaerligBegrunnelseFritekst(behandlingsresultat.vilkaarsresultater))
            .nyVurderingBakgrunn(brevbestilling.nyVurderingBakgrunn)
            .innledningFritekst(brevbestilling.innledningFritekst)
            .begrunnelseFritekst(brevbestilling.begrunnelseFritekst)
            .trygdeavgiftFritekst(brevbestilling.trygdeavgiftFritekst)
            .arbeidsgivere(hentArbeidsgivere(brevbestilling.behandling))
            .arbeidsland(dokgenMapperDatahenter.hentLandnavnFraLandkode(arbeidsland))
            .trygdeavtaleMedArbeidsland(harTrygdeavtaleMedArbeidsland(arbeidsland))
            .betalerArbeidsgiveravgift(erBetalerArbeidsgiveravgift(medlemAvFolketrygden.medlemskapsperioder))
            .build()
    }

    private fun mapAvgiftsPerioder(medlemAvFolketrygden: MedlemAvFolketrygden): List<AvgiftsperiodeDto> =
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.map {
            AvgiftsperiodeDto(
                it.periodeFra,
                it.periodeTil,
                it.trygdesats,
                it.trygdeavgiftsbeløpMd.verdi,
                it.grunnlagInntekstperiode.type,
                it.grunnlagInntekstperiode.avgiftspliktigInntektMnd?.verdi ?: BigDecimal.ZERO,
            )
        }.sortedByDescending { it.fom }

    private fun mapMedlemskapsPerioder(medlemAvFolketrygden: MedlemAvFolketrygden): List<no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto> =
        medlemAvFolketrygden.medlemskapsperioder.map {
            no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto(
                it.fom,
                it.tom,
                it.trygdedekning,
                it.innvilgelsesresultat
            )
        }.sortedByDescending { it.fom }

    private fun erBetalerArbeidsgiveravgift(medlemskapsperioder: Collection<Medlemskapsperiode>) =
        medlemskapsperioder.any { it.trygdeavgiftsperioder.any { it.grunnlagInntekstperiode.isArbeidsgiversavgiftBetalesTilSkatt } }

    private fun finnFullmektigTrygdeavgift(behandling: Behandling): String? {
        if (behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT).isEmpty) return null

        return trygdeavgiftsberegningService.finnFakturamottakerNavn(behandling.id)
    }

    private fun hentBegrunnelse(vilkaarsresultater: Set<Vilkaarsresultat>): Kodeverk? =
        hentBegrunnelse2_7(vilkaarsresultater) ?: hentBegrunnelse2_8(vilkaarsresultater)

    private fun hentBegrunnelse2_7(vilkaarsresultater: Set<Vilkaarsresultat>): Ftrl_2_7_begrunnelser? =
        vilkaarsresultater
            .filter { it.vilkaar == Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING }
            .map { it.begrunnelser.iterator().next().kode }
            .map { Ftrl_2_7_begrunnelser.valueOf(it) }
            .firstOrNull()

    private fun hentBegrunnelse2_8(vilkaarsresultater: Set<Vilkaarsresultat>): Ftrl_2_8_naer_tilknytning_norge_begrunnelser? =
        vilkaarsresultater
            .filter { it.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE }
            .map { it.begrunnelser.iterator().next().kode }
            .map { Ftrl_2_8_naer_tilknytning_norge_begrunnelser.valueOf(it) }
            .firstOrNull()

    private fun hentSaerligBegrunnelseFritekst(vilkaarsresultater: Set<Vilkaarsresultat>): String? =
        vilkaarsresultater
            .filter { it.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE || it.vilkaar == Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING }
            .map { it.begrunnelser.iterator().next().vilkaarsresultat.begrunnelseFritekst }
            .firstOrNull()

    private fun hentArbeidsgivere(behandling: Behandling): List<String> = (
        avklarteVirksomheterService.hentNorskeArbeidsgivere(behandling) +
            avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling) +
            avklarteVirksomheterService.hentNorskeSelvstendigeForetak(behandling)
        ).map { it.navn }

    private fun harTrygdeavtaleMedArbeidsland(arbeidsland: String): Boolean =
        Trygdeavtale_myndighetsland.values().any() { it.name == arbeidsland }

    private fun Medlemskapsperiode.harFullDekning(): Boolean = listOf(
        Trygdedekninger.FULL_DEKNING,
        Trygdedekninger.FULL_DEKNING_EOSFO,
        Trygdedekninger.FULL_DEKNING_FTRL
    ).contains(trygdedekning)

    private fun Medlemskapsperiode.harHelsedelDekning(): Boolean = listOf(
        Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
    ).contains(trygdedekning)

    private fun Medlemskapsperiode.fomErFør(instant: Instant): Boolean =
        this.fom.isBefore(LocalDate.ofInstant(instant, ZoneId.systemDefault()))
}
