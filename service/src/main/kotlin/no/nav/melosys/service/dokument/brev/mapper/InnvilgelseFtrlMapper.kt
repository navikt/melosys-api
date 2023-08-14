package no.nav.melosys.service.dokument.brev.mapper

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Representerer
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Ftrl_2_8_naer_tilknytning_norge_begrunnelser
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseFtrl
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.Periode
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.transaction.Transactional

@Component
class InnvilgelseFtrlMapper(
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter
) {
    @Transactional
    fun map(brevbestilling: InnvilgelseBrevbestilling): InnvilgelseFtrl {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val arbeidsland =
            behandlingsresultat.behandling.mottatteOpplysninger.mottatteOpplysningerData.soeknadsland.landkoder[0]

        return InnvilgelseFtrl.Builder(brevbestilling)
            .perioder(
                medlemAvFolketrygden.medlemskapsperioder
                    .flatMap { Periode.av(it) }
                    .toList()
            )
            .bestemmelse(medlemAvFolketrygden.bestemmelse)
            .avslåttHelsedelFørMottaksdato(
                erAvslåttHelsedelFørMottaksdato(
                    brevbestilling.forsendelseMottatt,
                    medlemAvFolketrygden
                )
            )
            .trygdeavgiftMottaker(medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftMottaker)
            .skatteplikttype(medlemAvFolketrygden.utledSkatteplikttype())
            .ftrl_2_8_begrunnelse(hentFtrlNærTilknytningNorgeBegrunnelse(behandlingsresultat.vilkaarsresultater))
            .begrunnelseAnnenGrunnFritekst(hentSaerligBegrunnelseFritekst(behandlingsresultat.vilkaarsresultater))
            .arbeidsgivere(
                avklarteVirksomheterService.hentNorskeArbeidsgivere(brevbestilling.behandling).map { it.navn })
            .arbeidsland(dokgenMapperDatahenter.hentLandnavnFraLandkode(arbeidsland))
            .trygdeavtaleMedArbeidsland(harTrygdeavtaleMedArbeidsland(arbeidsland))
            .arbeidsgiverFullmektigNavn(
                dokgenMapperDatahenter.hentFullmektigNavn(
                    brevbestilling.behandling.fagsak,
                    Representerer.ARBEIDSGIVER
                )
            )
            .betalerArbeidsgiveravgift(erBetalerArbeidsgiveravgift(medlemAvFolketrygden.medlemskapsperioder))
            .build()
    }

    private fun erAvslåttHelsedelFørMottaksdato(
        mottaksdato: Instant,
        medlemAvFolketrygden: MedlemAvFolketrygden
    ): Boolean =
        medlemAvFolketrygden.medlemskapsperioder.any {
            it.innvilgelsesresultat == InnvilgelsesResultat.AVSLAATT
                && it.fom.isBefore(LocalDate.ofInstant(mottaksdato, ZoneId.systemDefault()))
        }

    private fun erBetalerArbeidsgiveravgift(medlemskapsperioder: Collection<Medlemskapsperiode>) =
        // TODO("Venter på avklaring om dette faktisk stemmer: https://navno.sharepoint.com/:w:/r/sites/TeamMelosys/Shared%20Documents/Fag/Brev/Brev%20-%20folketrygdloven/Vedtak/Vedtak%20om%20innvilgelse%20av%20frivillig%20medlemskap%20i%20folketrygden%20V.2%20.docx?d=wc848a8c72d714c3bb5b832e32e5db907&csf=1&web=1&e=JnIG1L&nav=eyJjIjoyMDE4MjI1NTMwfQ")
        medlemskapsperioder.any { it.trygdeavgiftsperioder.any { it.grunnlagInntekstperiode.isArbeidsgiversavgiftBetalesTilSkatt } }

    private fun hentFtrlNærTilknytningNorgeBegrunnelse(vilkaarsresultater: Set<Vilkaarsresultat>): Ftrl_2_8_naer_tilknytning_norge_begrunnelser? =
        vilkaarsresultater
            .filter { it.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE }
            .map { it.begrunnelser.iterator().next().kode }
            .map { Ftrl_2_8_naer_tilknytning_norge_begrunnelser.valueOf(it) }
            .firstOrNull()

    private fun hentSaerligBegrunnelseFritekst(vilkaarsresultater: Set<Vilkaarsresultat>): String? =
        vilkaarsresultater
            .filter { it.vilkaar == Vilkaar.FTRL_2_8_NÆR_TILKNYTNING_NORGE }
            .map { it.begrunnelser.iterator().next().vilkaarsresultat.begrunnelseFritekst }
            .firstOrNull()

    private fun harTrygdeavtaleMedArbeidsland(arbeidsland: String): Boolean =
        Trygdeavtale_myndighetsland.values().any() { it.name == arbeidsland }
}
