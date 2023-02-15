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
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.*
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.*
import javax.transaction.Transactional

@Component
class InnvilgelseFtrlMapper(
    private val trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService,
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter
) {
    @Transactional
    fun map(brevbestilling: InnvilgelseBrevbestilling): InnvilgelseFtrl {
        val behandlingId = brevbestilling.behandlingId
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandlingId)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentAvgiftsgrunnlag(behandlingId)

        //NOTE Henter i første versjon av FTRL kun en norsk arbeidsgiver og forventer ett registert arbeidsland
        val norskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(brevbestilling.behandling)[0]
        val mottatteOpplysningerData = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData
        val soeknadsland = mottatteOpplysningerData.soeknadsland
        val arbeidsland = soeknadsland.landkoder[0]
        val medlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder
        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift
        return InnvilgelseFtrl.Builder(brevbestilling)
            .perioder(
                medlemskapsperioder
                    .map { m: Medlemskapsperiode? -> Periode(m) }
                    .toList()
            )
            .erFullstendigInnvilget(erFullstendigInnvilget(medlemskapsperioder))
            .ftrl_2_8_begrunnelse(hentSaerligBegrunnelse(behandlingsresultat))
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
        return medlemskapsperioder
            .all { p: Medlemskapsperiode -> p.innvilgelsesresultat == InnvilgelsesResultat.INNVILGET }
    }

    private fun hentSaerligBegrunnelse(behandlingsresultat: Behandlingsresultat): String? {
        return behandlingsresultat.vilkaarsresultater
            .filter { v: Vilkaarsresultat -> v.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE }
            .map { vilkaarsresultat: Vilkaarsresultat -> vilkaarsresultat.begrunnelser.iterator().next().kode }
            .firstOrNull()
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
            utenlandsk = utenlandsk
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
}
