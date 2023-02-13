package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoNorge
import no.nav.melosys.domain.avgift.AvgiftsgrunnlagInfoUtland
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Medfolgende_barn_begrunnelser
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.*
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.avklartefakta.AvklarteMedfolgendeFamilieService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.representant.RepresentantService
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.time.LocalDate
import java.util.*
import java.util.stream.Stream
import javax.transaction.Transactional

@Component
class InnvilgelseFtrlMapper(
    private val trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService,
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val avklarteMedfolgendeFamilieService: AvklarteMedfolgendeFamilieService,
    private val representantService: RepresentantService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter
) {
    @Transactional
    fun map(brevbestilling: InnvilgelseBrevbestilling): InnvilgelseFtrl {
        val behandlingId = brevbestilling.behandlingId
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandlingId)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingId)
        val avklarteMedfolgendeBarn = avklarteMedfolgendeFamilieService.hentAvklarteMedfølgendeBarn(behandlingId)
        val avklarteMedfolgendeEktefelle =
            avklarteMedfolgendeFamilieService.hentAvklartMedfølgendeEktefelle(behandlingId)

        //NOTE Henter i første versjon av FTRL kun en norsk arbeidsgiver og forventer ett registert arbeidsland
        val norskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(brevbestilling.behandling)[0]
        val mottatteOpplysningerData = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData
        val soeknadsland = mottatteOpplysningerData.soeknadsland
        val arbeidsland = soeknadsland.landkoder[0]
        val medlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder
        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift
        return InnvilgelseFtrl.Builder(brevbestilling)
            .perioder(
                medlemskapsperioder.stream()
                    .map { m: Medlemskapsperiode? -> Periode(m) }
                    .toList()
            )
            .erFullstendigInnvilget(erFullstendigInnvilget(medlemskapsperioder))
            .ftrl_2_8_begrunnelse(hentSaerligBegrunnelse(behandlingsresultat))
            .vurderingMedlemskapEktefelle(avklarteMedfolgendeEktefelle.finnes())
            .vurderingLovvalgBarn(avklarteMedfolgendeBarn.finnes())
            .omfattetFamilie(
                mapOmfattetFamilie(
                    behandlingId,
                    avklarteMedfolgendeEktefelle.familieOmfattetAvNorskTrygd,
                    avklarteMedfolgendeBarn.familieOmfattetAvNorskTrygd
                )
            )
            .ikkeOmfattetEktefelle(
                mapIkkeOmfattetEktefelle(
                    behandlingId,
                    avklarteMedfolgendeEktefelle.familieIkkeOmfattetAvNorskTrygd
                )
            )
            .ikkeOmfattetBarn(
                mapIkkeOmfattetBarn(
                    behandlingId,
                    avklarteMedfolgendeBarn.familieIkkeOmfattetAvNorskTrygd
                )
            )
            .arbeidsgiverNavn(norskeArbeidsgivere.navn)
            .arbeidsland(dokgenMapperDatahenter.hentLandnavnFraLandkode(arbeidsland))
            .trygdeavtaleMedArbeidsland(harTrygdeavtaleMedArbeidsland(arbeidsland))
            .vurderingTrygdeavgift(mapVurderingTrygdeavgift(trygdeavgiftsgrunnlag, fastsattTrygdeavgift))
            .loennsforhold(trygdeavgiftsgrunnlag.lønnsforhold.kode)
            .arbeidsgiverFullmektigNavn(
                dokgenMapperDatahenter.hentFullmektigNavn(
                    brevbestilling.behandling.fagsak,
                    Representerer.ARBEIDSGIVER
                )
            )
            .avgiftssatsAar(LocalDate.now().year.toString())
            .loennNorgeSkattepliktig(harLønnNorgeSkattepliktigNorge(trygdeavgiftsgrunnlag.avgiftsGrunnlagNorge))
            .loennUtlandSkattepliktig(harLønnUtlandSkattepliktigNorge(trygdeavgiftsgrunnlag.avgiftsGrunnlagUtland))
            .build()
    }

    private fun erFullstendigInnvilget(medlemskapsperioder: Collection<Medlemskapsperiode>): Boolean {
        return medlemskapsperioder.stream()
            .allMatch { p: Medlemskapsperiode -> p.innvilgelsesresultat == InnvilgelsesResultat.INNVILGET }
    }

    private fun hentSaerligBegrunnelse(behandlingsresultat: Behandlingsresultat): String? {
        return behandlingsresultat.vilkaarsresultater.stream()
            .findFirst()
            .filter { v: Vilkaarsresultat -> v.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE }
            .map { vilkaarsresultat: Vilkaarsresultat -> vilkaarsresultat.begrunnelser.iterator().next().kode }
            .orElse(null)
    }

    private fun mapOmfattetFamilie(
        behandlingID: Long,
        omfattetEktefelle: Set<OmfattetFamilie>,
        omfattetBarn: Set<OmfattetFamilie>
    ): List<FamiliemedlemInfo> {
        val medfolgendeEktefelle = avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingID)
        val medfolgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID)
        return Stream.concat(
            omfattetEktefelle.stream()
                .map { ektefelle: OmfattetFamilie -> tilFamiliemedlemInfo(medfolgendeEktefelle, ektefelle.uuid) },
            omfattetBarn.stream()
                .map { barn: OmfattetFamilie -> tilFamiliemedlemInfo(medfolgendeBarn, barn.uuid) }
        ).toList()
    }

    private fun mapIkkeOmfattetBarn(
        behandlingID: Long,
        barnIkkeOmfattetAvNorskTrygd: Set<IkkeOmfattetFamilie>
    ): List<IkkeOmfattetBarn> {
        val medfoelgendeBarn = avklarteMedfolgendeFamilieService.hentMedfølgendeBarn(behandlingID)
        return barnIkkeOmfattetAvNorskTrygd.stream()
            .map { ikkeOmfattetBarn: IkkeOmfattetFamilie ->
                IkkeOmfattetBarn(
                    tilFamiliemedlemInfo(medfoelgendeBarn, ikkeOmfattetBarn.uuid),
                    Medfolgende_barn_begrunnelser.valueOf(ikkeOmfattetBarn.begrunnelse)
                )
            }
            .toList()
    }

    private fun mapIkkeOmfattetEktefelle(
        behandlingId: Long,
        ektefelleIkkeOmfattet: Set<IkkeOmfattetFamilie>
    ): IkkeOmfattetEktefelle? {
        return ektefelleIkkeOmfattet.stream()
            .findFirst()
            .map { ikkeOmfattet: IkkeOmfattetFamilie ->
                IkkeOmfattetEktefelle(
                    tilFamiliemedlemInfo(
                        avklarteMedfolgendeFamilieService.hentMedfølgendEktefelle(behandlingId),
                        ikkeOmfattet.uuid
                    ),
                    ikkeOmfattet.begrunnelse
                )
            }
            .orElse(null)
    }

    private fun tilFamiliemedlemInfo(
        avklartMedfolgende: Map<String, MedfolgendeFamilie>,
        uuid: String
    ): FamiliemedlemInfo {
        val medfolgendeFamilie = Optional.ofNullable(avklartMedfolgende[uuid])
            .orElseThrow { FunksjonellException("Avklart medfølgende familie $uuid finnes ikke i mottatteOpplysningeret") }
        val sammensattNavn = if (medfolgendeFamilie!!.fnr != null) dokgenMapperDatahenter.hentSammensattNavn(
            medfolgendeFamilie.fnr
        ) else medfolgendeFamilie.navn
        return FamiliemedlemInfo(sammensattNavn, medfolgendeFamilie.fnr, medfolgendeFamilie.utledIdentType())
    }

    private fun mapVurderingTrygdeavgift(
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag,
        fastsattTrygdeavgift: FastsattTrygdeavgift
    ): VurderingTrygdeavgift {
        var norsk: TrygdeavgiftInfo? = null
        var utenlandsk: TrygdeavgiftInfo? = null
        if (trygdeavgiftsgrunnlag.avgiftsGrunnlagNorge != null) {
            val avgiftsGrunnlagNorge = trygdeavgiftsgrunnlag.avgiftsGrunnlagNorge
            norsk = TrygdeavgiftInfo(
                Optional.ofNullable(fastsattTrygdeavgift.avgiftspliktigNorskInntektMnd).orElse(0L),
                avgiftsGrunnlagNorge.erAvgiftspliktig(),
                avgiftsGrunnlagNorge.erSkattepliktig(),
                avgiftsGrunnlagNorge.betalerArbeidsgiverAvgift(),
                if (avgiftsGrunnlagNorge.særligAvgiftsgruppe != null) avgiftsGrunnlagNorge.særligAvgiftsgruppe.kode else null
            )
        }
        if (trygdeavgiftsgrunnlag.avgiftsGrunnlagUtland != null) {
            val avgiftsGrunnlagUtland = trygdeavgiftsgrunnlag.avgiftsGrunnlagUtland
            utenlandsk = TrygdeavgiftInfo(
                Optional.ofNullable(fastsattTrygdeavgift.avgiftspliktigUtenlandskInntektMnd).orElse(0L),
                avgiftsGrunnlagUtland.erAvgiftspliktig(),
                avgiftsGrunnlagUtland.erSkattepliktig(),
                avgiftsGrunnlagUtland.betalerArbeidsgiverAvgift(),
                if (avgiftsGrunnlagUtland.særligAvgiftsgruppe != null) avgiftsGrunnlagUtland.særligAvgiftsgruppe.kode else null
            )
        }
        return VurderingTrygdeavgift(
            norsk = norsk,
            utenlandsk = utenlandsk,
            selvbetalende = fastsattTrygdeavgift.betalesAv.rolle == Aktoersroller.BRUKER,
            representantNavn = hentRepresentantNavn(fastsattTrygdeavgift.representantNr)
        )
    }

    private fun harLønnNorgeSkattepliktigNorge(avgiftsgrunnlagInfoNorge: AvgiftsgrunnlagInfoNorge?): Boolean {
        return avgiftsgrunnlagInfoNorge != null && avgiftsgrunnlagInfoNorge.erSkattepliktig()
    }

    private fun harLønnUtlandSkattepliktigNorge(avgiftsgrunnlagInfoUtland: AvgiftsgrunnlagInfoUtland?): Boolean {
        return avgiftsgrunnlagInfoUtland != null && avgiftsgrunnlagInfoUtland.erSkattepliktig()
    }

    private fun harTrygdeavtaleMedArbeidsland(arbeidsland: String): Boolean {
        return Arrays.stream(Trygdeavtale_myndighetsland.values())
            .anyMatch { a: Trygdeavtale_myndighetsland -> a.name == arbeidsland }
    }

    private fun hentRepresentantNavn(representantNr: String): String? {
        return if (StringUtils.hasText(representantNr)) {
            representantService.hentRepresentant(representantNr).navn()
        } else null
    }
}
