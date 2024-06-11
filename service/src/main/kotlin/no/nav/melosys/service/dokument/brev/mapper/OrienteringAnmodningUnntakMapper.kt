package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.avklartefakta.AvklartYrkesgruppeType
import no.nav.melosys.domain.brev.OrienteringAnmodningUnntakBrevbestilling
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.integrasjon.dokgen.dto.OrienteringAnmodningUnntak
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import org.springframework.stereotype.Component

@Component
class OrienteringAnmodningUnntakMapper(
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val vilkaarsresultatService: VilkaarsresultatService,
    private val landvelgerService: LandvelgerService
) {
    @Transactional
    internal fun map(brevbestilling: OrienteringAnmodningUnntakBrevbestilling): OrienteringAnmodningUnntak {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val behandlingID = behandlingsresultat.behandling.id
        val anmodningsperiode = behandlingsresultat.hentAnmodningsperiode()
        val periodeFom = anmodningsperiode.fom
        val periodeTom = anmodningsperiode.tom
        val arbeidsland = landvelgerService.hentArbeidsland(behandlingID).beskrivelse

        val erDirekteTilAnmodningOmUnntak =
            behandlingsresultat.avklartefakta.find { it.fakta == AvklartYrkesgruppeType.ORDINAER_UTEN_ART12.name } != null

        val erAnmodningOmUnntakViaArbeidstaker = erGyldigVilkaar(
            behandlingID,
            listOf(
                Vilkaar.FO_883_2004_ART12_1,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_1,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART16_1
            )
        )
        val erAnmodningOmUnntakViaNæringsdrivende = erGyldigVilkaar(
            behandlingID,
            listOf(
                Vilkaar.FO_883_2004_ART12_2,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART14_2,
                Vilkaar.KONV_EFTA_STORBRITANNIA_ART16_3
            )
        )

        val lovvalgsbestemmelse = anmodningsperiode.bestemmelse.name()
        val begrunnelser = hentBegrunnelser(behandlingID)
        val erDirekteTilAnmodning = behandlingsresultat.avklartefakta.find { it.fakta == AvklartYrkesgruppeType.ORDINAER_UTEN_ART12.name } != null

        val direkteTilAnmodningBegrunnelser = if (erDirekteTilAnmodning) hentAnmodningBegrunnelser(behandlingID) else listOf()
        val anmodningBegrunnelser = if (!erDirekteTilAnmodning) hentAnmodningBegrunnelser(behandlingID) else listOf()

        return OrienteringAnmodningUnntak(
            brevbestilling,
            periodeFom,
            periodeTom,
            arbeidsland,
            erDirekteTilAnmodningOmUnntak,
            erAnmodningOmUnntakViaArbeidstaker,
            erAnmodningOmUnntakViaNæringsdrivende,
            lovvalgsbestemmelse,
            begrunnelser,
            direkteTilAnmodningBegrunnelser,
            anmodningBegrunnelser,
            brevbestilling.fritekst
        )
    }

    private fun erGyldigVilkaar(behandlingID: Long, vilkaarListe: List<Vilkaar>): Boolean {
        return vilkaarsresultatService.oppfyllerVilkaar(behandlingID, vilkaarListe)
    }

    private fun hentVilkårsresultat(behandlingID: Long, vilkaar: Vilkaar): Vilkaarsresultat? {
        return vilkaarsresultatService.finnVilkaarsresultat(behandlingID, vilkaar)
    }

    private fun hentBegrunnelser(behandlingID: Long): List<String> {
        val UTSENDT_ARBEIDSTAKER_begrunnelser =
            vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(behandlingID)?.begrunnelser?.stream()?.toList()?.map { it.kode }.orEmpty()
        val UTSENDT_NÆRINGSDRIVENDE_begrunnelser =
            vilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(behandlingID)?.begrunnelser?.stream()?.toList()?.map { it.kode }
                .orEmpty()
        val FORUTGÅENDE_MEDL_begrunnelser =
            hentVilkårsresultat(behandlingID, Vilkaar.FORUTGAAENDE_MEDLEMSKAP)?.begrunnelser?.stream()?.toList()?.map { it.kode }.orEmpty()
        val NORMALT_VIRKSOMHET_begrunnelser =
            hentVilkårsresultat(behandlingID, Vilkaar.NORMALT_DRIVER_VIRKSOMHET)?.begrunnelser?.stream()?.toList()?.map { it.kode }.orEmpty()
        val VESENTLIG_VIRKSOMHET_begrunnelser =
            hentVilkårsresultat(behandlingID, Vilkaar.VESENTLIG_VIRKSOMHET)?.begrunnelser?.stream()?.toList()?.map { it.kode }.orEmpty()


        return (UTSENDT_ARBEIDSTAKER_begrunnelser +
            UTSENDT_NÆRINGSDRIVENDE_begrunnelser +
            FORUTGÅENDE_MEDL_begrunnelser +
            NORMALT_VIRKSOMHET_begrunnelser +
            VESENTLIG_VIRKSOMHET_begrunnelser).toSet().toList()
    }


    private fun hentAnmodningBegrunnelser(behandlingID: Long): List<String> {
        return vilkaarsresultatService.finnUnntaksVilkaarsresultat(behandlingID)?.begrunnelser?.stream()?.toList()?.map { it.kode }.orEmpty()
    }
}
