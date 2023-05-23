package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Representerer
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.Periode
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.transaction.Transactional

@Component
class InnvilgelseFtrlMapper(
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter
) {
    @Transactional
    fun map(brevbestilling: InnvilgelseBrevbestilling): InnvilgelseFtrl {
        val behandlingId = brevbestilling.behandlingId
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(behandlingId)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden

        //NOTE Henter i første versjon av FTRL kun en norsk arbeidsgiver og forventer ett registert arbeidsland
        val norskeArbeidsgivere = avklarteVirksomheterService.hentNorskeArbeidsgivere(brevbestilling.behandling)[0]
        val mottatteOpplysningerData = behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData
        val soeknadsland = mottatteOpplysningerData.soeknadsland
        val arbeidsland = soeknadsland.landkoder[0]
        val medlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder
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
            .arbeidsgiverFullmektigNavn(
                dokgenMapperDatahenter.hentFullmektigNavn(
                    brevbestilling.behandling.fagsak,
                    Representerer.ARBEIDSGIVER
                )
            )
            .avgiftssatsAar(LocalDate.now().year.toString())
            .build()
    }

    private fun erFullstendigInnvilget(medlemskapsperioder: Collection<Medlemskapsperiode>): Boolean =
        medlemskapsperioder.all { it.innvilgelsesresultat == InnvilgelsesResultat.INNVILGET }

    private fun hentSaerligBegrunnelse(behandlingsresultat: Behandlingsresultat): String? =
        behandlingsresultat.vilkaarsresultater
            .filter { it.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE }
            .map { it.begrunnelser.iterator().next().kode }
            .firstOrNull()

    private fun harTrygdeavtaleMedArbeidsland(arbeidsland: String): Boolean =
        Trygdeavtale_myndighetsland.values().any() { it.name == arbeidsland }
}
