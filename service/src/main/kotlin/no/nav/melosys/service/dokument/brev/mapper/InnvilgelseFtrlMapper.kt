package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.brev.InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrlIkkeYrkesaktivFrivillig
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrlIkkeYrkesaktivPliktig
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrlYrkesaktivFrivillig
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseYrkesaktivPliktigFtrl
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftsperiodeDto
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import jakarta.transaction.Transactional
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift

@Component
class InnvilgelseFtrlMapper(
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
) {
    @Transactional
    internal fun mapYrkesaktivFrivillig(brevbestilling: InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling): InnvilgelseFtrlYrkesaktivFrivillig {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val søknadsland = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val avslåttMedlemskapsperiodeFørMottaksdatoHelsedel =
            mapAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel(medlemAvFolketrygden, brevbestilling.forsendelseMottatt)
        val avslåttMedlemskapsperiodeFørMottaksdatoFullDekning =
            mapAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning(medlemAvFolketrygden, brevbestilling.forsendelseMottatt)

        return InnvilgelseFtrlYrkesaktivFrivillig(
            brevbestilling = brevbestilling,
            behandlingstype = behandlingsresultat.behandling.type,
            avgiftsperioder = mapAvgiftsPerioder(medlemAvFolketrygden),
            medlemskapsperioder = mapMedlemskapsPerioder(medlemAvFolketrygden),
            bestemmelse = medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget() }.sortedBy { it.fom }.first().bestemmelse,
            avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = avslåttMedlemskapsperiodeFørMottaksdatoHelsedel,
            avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = avslåttMedlemskapsperiodeFørMottaksdatoFullDekning,
            trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandlingsresultat.behandling),
            skatteplikttype = medlemAvFolketrygden.utledSkatteplikttype(),
            begrunnelse = hentBegrunnelse(behandlingsresultat.vilkaarsresultater),
            begrunnelseAnnenGrunnFritekst = hentSaerligBegrunnelseFritekst(behandlingsresultat.vilkaarsresultater),
            nyVurderingBakgrunn = brevbestilling.nyVurderingBakgrunn,
            innledningFritekst = brevbestilling.innledningFritekst,
            begrunnelseFritekst = brevbestilling.begrunnelseFritekst,
            trygdeavgiftFritekst = brevbestilling.trygdeavgiftFritekst,
            arbeidsgivere = hentArbeidsgivere(brevbestilling.behandling),
            flereLandUkjentHvilke = søknadsland.isFlereLandUkjentHvilke,
            land = søknadsland.landkoder.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it) },
            trygdeavtaleLand = mapTrygdeavtaleLand(søknadsland.landkoder),
            betalerArbeidsgiveravgift = erBetalerArbeidsgiveravgift(medlemAvFolketrygden.fastsattTrygdeavgift)
        )
    }

    internal fun mapIkkeYrkesaktivFrivillig(brevbestilling: DokgenBrevbestilling): InnvilgelseFtrlIkkeYrkesaktivFrivillig {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandling.id)
        val søknadsland = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val avslåttMedlemskapsperiodeFørMottaksdatoHelsedel =
            mapAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel(behandlingsresultat.medlemAvFolketrygden, brevbestilling.forsendelseMottatt)
        val avslåttMedlemskapsperiodeFørMottaksdatoFullDekning =
            mapAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning(behandlingsresultat.medlemAvFolketrygden, brevbestilling.forsendelseMottatt)

        return InnvilgelseFtrlIkkeYrkesaktivFrivillig(
            brevbestilling = brevbestilling,
            flereLandUkjentHvilke = søknadsland.isFlereLandUkjentHvilke,
            land = søknadsland.landkoder.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it) },
            bestemmelse = behandlingsresultat.medlemAvFolketrygden.medlemskapsperioder.last().bestemmelse,
            nyVurderingBakgrunn = behandlingsresultat.nyVurderingBakgrunn,
            innledningFritekst = behandlingsresultat.innledningFritekst,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            ikkeYrkesaktivRelasjonType = hentAvklartFakta(behandlingsresultat, Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON),
            avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = avslåttMedlemskapsperiodeFørMottaksdatoHelsedel,
            avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = avslåttMedlemskapsperiodeFørMottaksdatoFullDekning,
            medlemskapsperioder = mapMedlemskapsPerioder(behandlingsresultat.medlemAvFolketrygden)
        )
    }

    internal fun mapIkkeYrkesaktivPliktig(brevbestilling: DokgenBrevbestilling): InnvilgelseFtrlIkkeYrkesaktivPliktig {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandling.id)
        val søknadsland = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val medlemskapsperiode = Periode(
            behandlingsresultat.medlemAvFolketrygden.utledMedlemskapsperiodeFom(),
            behandlingsresultat.medlemAvFolketrygden.utledMedlemskapsperiodeTom()
        )

        return InnvilgelseFtrlIkkeYrkesaktivPliktig(
            brevbestilling = brevbestilling,
            flereLandUkjentHvilke = søknadsland.isFlereLandUkjentHvilke,
            land = søknadsland.landkoder.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it) },
            bestemmelse = behandlingsresultat.medlemAvFolketrygden.medlemskapsperioder.last().bestemmelse,
            nyVurderingBakgrunn = behandlingsresultat.nyVurderingBakgrunn,
            innledningFritekst = behandlingsresultat.innledningFritekst,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            ikkeYrkesaktivOppholdType = hentAvklartFakta(behandlingsresultat, Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD),
            ikkeYrkesaktivRelasjonType = hentAvklartFakta(behandlingsresultat, Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON),
            medlemskapsperiode = medlemskapsperiode
        )
    }

    internal fun mapYrkesaktivPliktig(brevbestilling: DokgenBrevbestilling): InnvilgelseYrkesaktivPliktigFtrl {
        val behandling = brevbestilling.behandling
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandling.id)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val søknadsland = behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val medlemskapsperiode = medlemAvFolketrygden.medlemskapsperioder.single()
        val harLavSatsPgaAlder = harLavSatsPgaAlderIMinstEnPeriode(dokgenMapperDatahenter.hentPersondata(behandling).fødselsdato, medlemskapsperiode)

        return InnvilgelseYrkesaktivPliktigFtrl(
            brevbestilling = brevbestilling,
            behandlingstype = behandling.type,
            avgiftsperioder = mapAvgiftsPerioder(medlemAvFolketrygden),
            medlemskapsperiode = mapMedlemskapsPerioder(medlemAvFolketrygden).single(),
            bestemmelse = medlemskapsperiode.bestemmelse,
            harLavSatsPgaAlder = harLavSatsPgaAlder,
            arbeidssituasjontype = hentAvklartFakta(behandlingsresultat, Avklartefaktatyper.ARBEIDSSITUASJON),
            trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandling),
            skatteplikttype = medlemAvFolketrygden.utledSkatteplikttype(),
            begrunnelse = hentBegrunnelse(behandlingsresultat.vilkaarsresultater),
            begrunnelseAnnenGrunnFritekst = hentSaerligBegrunnelseFritekst(behandlingsresultat.vilkaarsresultater),
            nyVurderingBakgrunn = behandlingsresultat.nyVurderingBakgrunn,
            innledningFritekst = behandlingsresultat.innledningFritekst,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            trygdeavgiftFritekst = behandlingsresultat.trygdeavgiftFritekst,
            arbeidsgivere = hentArbeidsgivere(behandling),
            flereLandUkjentHvilke = søknadsland.isFlereLandUkjentHvilke,
            land = søknadsland.landkoder.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it) },
            trygdeavtaleLand = mapTrygdeavtaleLand(søknadsland.landkoder),
            betalerArbeidsgiveravgift = erBetalerArbeidsgiveravgift(medlemAvFolketrygden.fastsattTrygdeavgift),
        )
    }

    private fun hentAvklartFakta(behandlingsresultat: Behandlingsresultat, type: Avklartefaktatyper): String? =
        behandlingsresultat.avklartefakta.firstOrNull { it.type == type }?.fakta

    private fun harLavSatsPgaAlderIMinstEnPeriode(foedselsdato: LocalDate, medlemskapsperiode: Medlemskapsperiode): Boolean {
        val alderForInneværendeÅrForMedlemskapsperiodeFom = medlemskapsperiode.fom.year.minus(foedselsdato.year)
        val alderForInneværendeÅrForMedlemskapsperiodeTom = medlemskapsperiode.tom?.year?.minus(foedselsdato.year)

        return alderForInneværendeÅrForMedlemskapsperiodeFom !in 18..68
            || (alderForInneværendeÅrForMedlemskapsperiodeTom?.let { it !in 18..68 } ?: false)
    }

    private fun mapMedlemskapsPerioder(medlemAvFolketrygden: MedlemAvFolketrygden): List<no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto> =
        medlemAvFolketrygden.medlemskapsperioder.map {
            no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto(
                it.fom,
                it.tom,
                it.trygdedekning,
                it.innvilgelsesresultat
            )
        }.sortedByDescending { it.fom }

    private fun mapAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning(medlemAvFolketrygden: MedlemAvFolketrygden, mottattDato: Instant): Boolean =
        medlemAvFolketrygden.medlemskapsperioder.any { it.erAvslaatt() && it.harFullDekning() && it.fomErFør(mottattDato) }

    private fun mapAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel(medlemAvFolketrygden: MedlemAvFolketrygden, mottattDato: Instant): Boolean =
        medlemAvFolketrygden.medlemskapsperioder.any { it.erAvslaatt() && it.harHelsedelDekning() && it.fomErFør(mottattDato) }

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

    private fun mapTrygdeavtaleLand(landkoder: List<String>): List<String> =
        Trygdeavtale_myndighetsland.values().filter { landkoder.contains(it.kode) }.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it.kode) }

    private fun erBetalerArbeidsgiveravgift(fastsattTrygdeavgift: FastsattTrygdeavgift?) =
        fastsattTrygdeavgift?.trygdeavgiftsperioder?.any { it.grunnlagInntekstperiode.isArbeidsgiversavgiftBetalesTilSkatt } ?: false

    private fun finnFullmektigTrygdeavgift(behandling: Behandling): String? {
        if (behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) == null) return null

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

    private fun Medlemskapsperiode.harFullDekning(): Boolean = listOf(
        Trygdedekninger.FULL_DEKNING,
        Trygdedekninger.FULL_DEKNING_EOSFO,
        Trygdedekninger.FULL_DEKNING_FTRL
    ).contains(trygdedekning)

    private fun Medlemskapsperiode.harHelsedelDekning(): Boolean = listOf(
        Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
    ).contains(trygdedekning)

    private fun Medlemskapsperiode.fomErFør(instant: Instant): Boolean =
        this.fom.isBefore(LocalDate.ofInstant(instant, ZoneId.systemDefault()))
}
