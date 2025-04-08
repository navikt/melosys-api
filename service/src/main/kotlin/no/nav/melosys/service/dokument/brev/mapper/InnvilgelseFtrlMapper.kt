package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.brev.InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Vertslandsavtale_bestemmelser.TILLEGGSAVTALE_NATO
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_7_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.integrasjon.dokgen.dto.*
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftsperiodeDto
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avklartefakta.AvklartUkjentSluttdatoMedlemskapsperiodeService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
class InnvilgelseFtrlMapper(
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val avklartUkjentSluttdatoMedlemskapsperiodeService: AvklartUkjentSluttdatoMedlemskapsperiodeService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
) {

    @Transactional
    internal fun mapPensjonistFrivillig(brevbestilling: DokgenBrevbestilling): InnvilgelseFtrlPensjonistFrivillig {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val søknadsland = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val søknadNorgeEllerUtenforEØS = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData as SøknadNorgeEllerUtenforEØS
        val trygdedekning = søknadNorgeEllerUtenforEØS.trygdedekning
        val avslåttMedlemskapsIPensjonsdel =
            avslåttMedlemskapsMedFørsteLeddBPensjon(behandlingsresultat)

        val ukjentSluttdatoMedlemskapsperiode = hentUkjentSluttdatoMedlemskapsperiodeAvklartFakta(behandlingsresultat.behandling.id)
        val bestemmelse = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }.sortedBy { it.fom }.first().bestemmelse
        return InnvilgelseFtrlPensjonistFrivillig (
            brevbestilling = brevbestilling,
            behandlingstype = behandlingsresultat.behandling.type,
            medlemskapsperioder = mapMedlemskapsPerioder(behandlingsresultat),
            bestemmelse = bestemmelse,
            avgiftsperioder = mapAvgiftsperioderPensjonist(behandlingsresultat),
            avslåttMedlemskapsIPensjonsdel = listOf(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON,
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            ).contains(trygdedekning) && avslåttMedlemskapsIPensjonsdel,
            avslåttMedlemskapsIPensjonsdelMenIkkeHelsedel = listOf(
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON,
            ).contains(trygdedekning) && avslåttMedlemskapsIPensjonsdel,
            trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandlingsresultat.behandling),
            begrunnelse = hentBegrunnelse(behandlingsresultat.vilkaarsresultater),
            begrunnelseAnnenGrunnFritekst = hentSaerligBegrunnelseFritekst(behandlingsresultat.vilkaarsresultater),
            nyVurderingBakgrunn = behandlingsresultat.nyVurderingBakgrunn,
            innledningFritekst = behandlingsresultat.innledningFritekst,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            trygdeavgiftFritekst = behandlingsresultat.trygdeavgiftFritekst,
            flereLandUkjentHvilke = søknadsland.isFlereLandUkjentHvilke,
            land = søknadsland.landkoder.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it) },
            ukjentSluttdatoMedlemskapsperiode = ukjentSluttdatoMedlemskapsperiode,
            // TODO: Legger inn støtte for skalHaFaktura i https://jira.adeo.no/browse/MELOSYS-7251, avventer PR hvor implementasjon av dette pågår
            skalHaFaktura = false
        )
    }

    internal fun mapPensjonistPliktig(brevbestilling: DokgenBrevbestilling): InnvilgelsePensjonistPliktigFtrl {
        val behandling = brevbestilling.behandling
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandling.id)
        val søknadsland = behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val medlemskapsperiode = behandlingsresultat.medlemskapsperioder.single()
        val ukjentSluttdatoMedlemskapsperiode = hentUkjentSluttdatoMedlemskapsperiodeAvklartFakta(behandlingsresultat.behandling.id)

        return InnvilgelsePensjonistPliktigFtrl(
            brevbestilling = brevbestilling,
            behandlingstype = behandling.type,
            avgiftsperioder = mapAvgiftsPerioder(behandlingsresultat),
            medlemskapsperiode = mapMedlemskapsPerioder(behandlingsresultat).single(),
            bestemmelse = medlemskapsperiode.bestemmelse,
            trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandling),
            begrunnelse = hentBegrunnelse(behandlingsresultat.vilkaarsresultater),
            begrunnelseAnnenGrunnFritekst = hentSaerligBegrunnelseFritekst(behandlingsresultat.vilkaarsresultater),
            nyVurderingBakgrunn = behandlingsresultat.nyVurderingBakgrunn,
            innledningFritekst = behandlingsresultat.innledningFritekst,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            trygdeavgiftFritekst = behandlingsresultat.trygdeavgiftFritekst,
            flereLandUkjentHvilke = søknadsland.isFlereLandUkjentHvilke,
            land = søknadsland.landkoder.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it) },
            ukjentSluttdatoMedlemskapsperiode = ukjentSluttdatoMedlemskapsperiode
        )
    }


    @Transactional
    internal fun mapYrkesaktivFrivillig(brevbestilling: InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling): InnvilgelseFtrlYrkesaktivFrivillig {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val søknadsland = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val avslåttMedlemskapsperiodeFørMottaksdatoHelsedel =
            mapAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel(behandlingsresultat, brevbestilling.forsendelseMottatt)
        val avslåttMedlemskapsperiodeFørMottaksdatoFullDekning =
            mapAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning(behandlingsresultat, brevbestilling.forsendelseMottatt)
        val ukjentSluttdatoMedlemskapsperiode = hentUkjentSluttdatoMedlemskapsperiodeAvklartFakta(behandlingsresultat.behandling.id)

        return InnvilgelseFtrlYrkesaktivFrivillig(
            brevbestilling = brevbestilling,
            behandlingstype = behandlingsresultat.behandling.type,
            avgiftsperioder = mapAvgiftsPerioder(behandlingsresultat),
            medlemskapsperioder = mapMedlemskapsPerioder(behandlingsresultat),
            bestemmelse = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }.sortedBy { it.fom }.first().bestemmelse,
            avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = avslåttMedlemskapsperiodeFørMottaksdatoHelsedel,
            avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = avslåttMedlemskapsperiodeFørMottaksdatoFullDekning,
            trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandlingsresultat.behandling),
            skatteplikttype = behandlingsresultat.utledSkatteplikttype(),
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
            betalerArbeidsgiveravgift = erBetalerArbeidsgiveravgift(behandlingsresultat),
            ukjentSluttdatoMedlemskapsperiode = ukjentSluttdatoMedlemskapsperiode
        )
    }

    internal fun mapIkkeYrkesaktivFrivillig(brevbestilling: DokgenBrevbestilling): InnvilgelseFtrlIkkeYrkesaktivFrivillig {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandling.id)
        val søknadsland = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val avslåttMedlemskapsperiodeFørMottaksdatoHelsedel =
            mapAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel(behandlingsresultat, brevbestilling.forsendelseMottatt)
        val avslåttMedlemskapsperiodeFørMottaksdatoFullDekning =
            mapAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning(behandlingsresultat, brevbestilling.forsendelseMottatt)
        val ukjentSluttdatoMedlemskapsperiode = hentUkjentSluttdatoMedlemskapsperiodeAvklartFakta(behandlingsresultat.behandling.id)

        return InnvilgelseFtrlIkkeYrkesaktivFrivillig(
            brevbestilling = brevbestilling,
            flereLandUkjentHvilke = søknadsland.isFlereLandUkjentHvilke,
            land = søknadsland.landkoder.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it) },
            bestemmelse = behandlingsresultat.medlemskapsperioder.last().bestemmelse,
            nyVurderingBakgrunn = behandlingsresultat.nyVurderingBakgrunn,
            innledningFritekst = behandlingsresultat.innledningFritekst,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            ikkeYrkesaktivRelasjonType = hentAvklartFakta(behandlingsresultat, Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON),
            avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = avslåttMedlemskapsperiodeFørMottaksdatoHelsedel,
            avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = avslåttMedlemskapsperiodeFørMottaksdatoFullDekning,
            medlemskapsperioder = mapMedlemskapsPerioder(behandlingsresultat),
            ukjentSluttdatoMedlemskapsperiode = ukjentSluttdatoMedlemskapsperiode
        )
    }

    internal fun mapIkkeYrkesaktivPliktig(brevbestilling: DokgenBrevbestilling): InnvilgelseFtrlIkkeYrkesaktivPliktig {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandling.id)
        val søknadsland = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val medlemskapsperiode = Periode(
            behandlingsresultat.utledMedlemskapsperiodeFom(),
            behandlingsresultat.utledMedlemskapsperiodeTom()
        )
        val ukjentSluttdatoMedlemskapsperiode = hentUkjentSluttdatoMedlemskapsperiodeAvklartFakta(behandlingsresultat.behandling.id)

        return InnvilgelseFtrlIkkeYrkesaktivPliktig(
            brevbestilling = brevbestilling,
            flereLandUkjentHvilke = søknadsland.isFlereLandUkjentHvilke,
            land = søknadsland.landkoder.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it) },
            bestemmelse = behandlingsresultat.medlemskapsperioder.last().bestemmelse,
            nyVurderingBakgrunn = behandlingsresultat.nyVurderingBakgrunn,
            innledningFritekst = behandlingsresultat.innledningFritekst,
            begrunnelseFritekst = behandlingsresultat.begrunnelseFritekst,
            ikkeYrkesaktivOppholdType = hentAvklartFakta(behandlingsresultat, Avklartefaktatyper.IKKE_YRKESAKTIV_FTRL_2_1_OPPHOLD),
            ikkeYrkesaktivRelasjonType = hentAvklartFakta(behandlingsresultat, Avklartefaktatyper.IKKE_YRKESAKTIV_RELASJON),
            medlemskapsperiode = medlemskapsperiode,
            ukjentSluttdatoMedlemskapsperiode = ukjentSluttdatoMedlemskapsperiode
        )
    }

    internal fun mapYrkesaktivPliktig(brevbestilling: DokgenBrevbestilling): InnvilgelseYrkesaktivPliktigFtrl {
        val behandling = brevbestilling.behandling
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandling.id)
        val søknadsland = behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland
        val medlemskapsperiode = behandlingsresultat.medlemskapsperioder.single()
        val harLavSatsPgaAlder = medlemskapsperiode.bestemmelse != TILLEGGSAVTALE_NATO && harLavSatsPgaAlderIMinstEnPeriode(dokgenMapperDatahenter.hentPersondata(behandling).fødselsdato, medlemskapsperiode)
        val ukjentSluttdatoMedlemskapsperiode = hentUkjentSluttdatoMedlemskapsperiodeAvklartFakta(behandlingsresultat.behandling.id)

        return InnvilgelseYrkesaktivPliktigFtrl(
            brevbestilling = brevbestilling,
            behandlingstype = behandling.type,
            avgiftsperioder = mapAvgiftsPerioder(behandlingsresultat),
            medlemskapsperiode = mapMedlemskapsPerioder(behandlingsresultat).single(),
            bestemmelse = medlemskapsperiode.bestemmelse,
            harLavSatsPgaAlder = harLavSatsPgaAlder,
            arbeidssituasjontype = hentAvklartFakta(behandlingsresultat, Avklartefaktatyper.ARBEIDSSITUASJON),
            trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandling),
            skatteplikttype = behandlingsresultat.utledSkatteplikttype(),
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
            betalerArbeidsgiveravgift = erBetalerArbeidsgiveravgift(behandlingsresultat),
            ukjentSluttdatoMedlemskapsperiode = ukjentSluttdatoMedlemskapsperiode
        )
    }

    private fun hentUkjentSluttdatoMedlemskapsperiodeAvklartFakta(behandlingID: Long): Boolean {
        return avklartUkjentSluttdatoMedlemskapsperiodeService.hentUkjentSluttdatoMedlemskapsperiode(behandlingID) ?: false
    }

    private fun hentAvklartFakta(behandlingsresultat: Behandlingsresultat, type: Avklartefaktatyper): String? =
        behandlingsresultat.avklartefakta.firstOrNull { it.type == type }?.fakta

    private fun harLavSatsPgaAlderIMinstEnPeriode(foedselsdato: LocalDate, medlemskapsperiode: Medlemskapsperiode): Boolean {
        val alderForInneværendeÅrForMedlemskapsperiodeFom = medlemskapsperiode.fom.year.minus(foedselsdato.year)
        val alderForInneværendeÅrForMedlemskapsperiodeTom = medlemskapsperiode.tom?.year?.minus(foedselsdato.year)

        return alderForInneværendeÅrForMedlemskapsperiodeFom !in 17..68
            || (alderForInneværendeÅrForMedlemskapsperiodeTom?.let { it !in 17..68 } ?: false)
    }

    private fun mapMedlemskapsPerioder(behandlingsresultat: Behandlingsresultat): List<no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto> =
        behandlingsresultat.medlemskapsperioder.map {
            no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto(
                it.fom,
                it.tom,
                it.trygdedekning,
                it.innvilgelsesresultat
            )
        }.sortedByDescending { it.fom }

    private fun mapAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning(behandlingsresultat: Behandlingsresultat, mottattDato: Instant): Boolean =
        behandlingsresultat.medlemskapsperioder.any { it.erAvslaatt() && it.harFullDekning() && it.fomErFør(mottattDato) }

    private fun mapAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel(behandlingsresultat: Behandlingsresultat, mottattDato: Instant): Boolean =
        behandlingsresultat.medlemskapsperioder.any { it.erAvslaatt() && it.harHelsedelDekning() && it.fomErFør(mottattDato) }

    private fun avslåttMedlemskapsMedFørsteLeddBPensjon(behandlingsresultat: Behandlingsresultat): Boolean =
        behandlingsresultat.medlemskapsperioder.any {
            it.erAvslaatt() && it.trygdedekning == Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
        }


    private fun mapAvgiftsPerioder(behandlingsresultat: Behandlingsresultat): List<AvgiftsperiodeDto> {
        if (behandlingsresultat.trygdeavgiftsperioder.all {
                it.trygdeavgiftsbeløpMd.verdi == BigDecimal.ZERO && it.trygdesats == BigDecimal.ZERO
            }) {
            return emptyList()
        }

        return behandlingsresultat.trygdeavgiftsperioder.map {
            AvgiftsperiodeDto(
                it.periodeFra,
                it.periodeTil,
                it.trygdesats,
                it.trygdeavgiftsbeløpMd.verdi,
                it.grunnlagInntekstperiode!!.type,
                it.grunnlagInntekstperiode!!.avgiftspliktigMndInntekt?.verdi ?: BigDecimal.ZERO,
            )
        }.sortedByDescending { it.fom }
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
                inntektskilde = it.grunnlagInntekstperiode!!.type.beskrivelse,
                trygdedekning = it.grunnlagMedlemskapsperiodeNotNull.trygdedekning.beskrivelse,
                avgiftspliktigInntektPerMd = it.grunnlagInntekstperiode!!.avgiftspliktigMndInntekt?.verdi ?: BigDecimal.ZERO,
                arbeidsgiveravgiftBetalt = SvarAlternativ.IKKE_RELEVANT,
                skatteplikt = it.grunnlagSkatteforholdTilNorge!!.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG
            )
        }.sortedByDescending { it.fom }
    }

    private fun mapTrygdeavtaleLand(landkoder: List<String>): List<String> =
        Trygdeavtale_myndighetsland.values().filter { landkoder.contains(it.kode) }.map { dokgenMapperDatahenter.hentLandnavnFraLandkode(it.kode) }

    private fun erBetalerArbeidsgiveravgift(behandlingsresultat: Behandlingsresultat): Boolean {
        val trygdeavgiftmottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat.id)
        if (trygdeavgiftmottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT) {
            return true
        }
        return behandlingsresultat.trygdeavgiftsperioder?.any { it.grunnlagInntekstperiode!!.isArbeidsgiversavgiftBetalesTilSkatt } ?: false
    }

    private fun finnFullmektigTrygdeavgift(behandling: Behandling): String? {
        if (behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) == null) return null

        return trygdeavgiftsberegningService.finnFakturamottakerNavn(behandling.id)
    }

    private fun hentBegrunnelse(vilkaarsresultater: Set<Vilkaarsresultat>): Kodeverk? =
        hentBegrunnelse2_7(vilkaarsresultater) ?: hentBegrunnelse2_8(vilkaarsresultater)

    private fun hentBegrunnelse2_7(vilkaarsresultater: Set<Vilkaarsresultat>): Ftrl_2_7_begrunnelser? =
        vilkaarsresultater
            .firstOrNull {
                it.vilkaar == Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING && it.begrunnelser.isNotEmpty()
            }
            ?.begrunnelser?.firstOrNull()?.kode
            ?.let { Ftrl_2_7_begrunnelser.valueOf(it) }

    private fun hentBegrunnelse2_8(vilkaarsresultater: Set<Vilkaarsresultat>): Ftrl_2_8_naer_tilknytning_norge_begrunnelser? =
        vilkaarsresultater
            .firstOrNull {
                it.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE && it.begrunnelser.isNotEmpty()
            }
            ?.begrunnelser?.firstOrNull()?.kode
            ?.let { Ftrl_2_8_naer_tilknytning_norge_begrunnelser.valueOf(it) }

    private fun hentSaerligBegrunnelseFritekst(vilkaarsresultater: Set<Vilkaarsresultat>): String? =
        vilkaarsresultater
            .firstOrNull {
                it.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE || it.vilkaar == Vilkaar.FTRL_2_7_RIMELIGHETSVURDERING
            }
            ?.begrunnelseFritekst

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
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE,
        Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE
    ).contains(trygdedekning)

    private fun Medlemskapsperiode.fomErFør(instant: Instant): Boolean =
        this.fom.isBefore(LocalDate.ofInstant(instant, ZoneId.systemDefault()))
}
