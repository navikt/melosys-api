package no.nav.melosys.service.dokument.brev.bygger

import no.nav.dok.melosysbrev._000115.BostedsadresseTypeKode
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.dokument.brev.BrevData
import no.nav.melosys.service.dokument.brev.BrevDataA001
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.vilkaar.VilkaarsresultatService
import org.apache.commons.collections4.ListUtils

//TODO verifiser null safety.
class BrevDataByggerA001(
    private val lovvalgsperiodeService: LovvalgsperiodeService,
    private val anmodningsperiodeService: AnmodningsperiodeService,
    private val utenlandskMyndighetService: UtenlandskMyndighetService,
    private val vilkaarsresultatService: VilkaarsresultatService
) : BrevDataBygger {

    private lateinit var dataGrunnlag: BrevDataGrunnlag
    private lateinit var behandling: Behandling

    override fun lag(dataGrunnlag: BrevDataGrunnlag, saksbehandler: String): BrevData {
        this.dataGrunnlag = dataGrunnlag
        this.behandling = dataGrunnlag.behandling

        var anmodningsperioder = hentAnmodningsperioder()
        val landkode = Land_iso2.valueOf(anmodningsperioder.first().unntakFraLovvalgsland.kode)

        val brevData = BrevDataA001().apply {
            persondata = dataGrunnlag.person
            utenlandskMyndighet = utenlandskMyndighetService.hentUtenlandskMyndighet(landkode)
            arbeidsgivendeVirksomheter = ListUtils.union(
                dataGrunnlag.avklarteVirksomheterGrunnlag.hentNorskeArbeidsgivere(),
                dataGrunnlag.avklarteVirksomheterGrunnlag.hentUtenlandskeArbeidsgivere()
            )
            selvstendigeVirksomheter = ListUtils.union(
                dataGrunnlag.avklarteVirksomheterGrunnlag.hentNorskeSelvstendige(),
                dataGrunnlag.avklarteVirksomheterGrunnlag.hentUtenlandskeSelvstendige()
            )

            val adresseOgType = hentBostedsadresseOgTypeKode()
            bostedsadresseTypeKode = adresseOgType.first
            bostedsadresse = adresseOgType.second

            arbeidssteder = dataGrunnlag.arbeidsstedGrunnlag.hentArbeidssteder()
            utenlandskIdent = hentUtenlandskIdent(landkode)
            anmodningsperioder = anmodningsperioder
            tidligereLovvalgsperioder = lovvalgsperiodeService.hentTidligereLovvalgsperioder(behandling)
            ansettelsesperiode = hentAnsettelsesperiode()

            val art16Vilkaar = hentVilkårsresultat()
            val art16VilkaarBegrunnelser = art16Vilkaar.begrunnelser
            if (vilkaarsresultatService.harVilkaarForArtikkel12(behandling.id)) {
                anmodningBegrunnelser = art16VilkaarBegrunnelser
                anmodningUtenArt12Begrunnelser = emptySet()
            } else {
                anmodningBegrunnelser = emptySet()
                anmodningUtenArt12Begrunnelser = art16VilkaarBegrunnelser
            }

            if (harSærligGrunn(art16VilkaarBegrunnelser)) {
                anmodningFritekstBegrunnelse = art16Vilkaar.begrunnelseFritekstEessi
            }
            ytterligereInformasjon = dataGrunnlag.brevbestilling.ytterligereInformasjon
        }
        return brevData
    }

    private fun harSærligGrunn(art16VilkaarBegrunnelser: Set<VilkaarBegrunnelse>): Boolean =
        art16VilkaarBegrunnelser.any { it.kode == "SAERLIG_GRUNN" }

    private fun hentVilkårsresultat(): Vilkaarsresultat =
        vilkaarsresultatService.finnVilkaarsresultat(behandling.id, Vilkaar.FO_883_2004_ART16_1)
            .orElseThrow { TekniskException("Fant ingen vilkårbegrunnelse for FO_883_2004_ART16_1") }
            .also {
                if (it.begrunnelser.isEmpty()) throw TekniskException("Brevet A001 trenger en begrunnelsekode for ART16_1")
            }

    private fun hentUtenlandskIdent(landkode: Land_iso2): String? =
        dataGrunnlag.mottatteOpplysningerData.personOpplysninger.utenlandskIdent
            .firstOrNull { it.landkode == landkode.kode }
            ?.ident

    private fun hentBostedsadresseOgTypeKode(): Pair<BostedsadresseTypeKode, StrukturertAdresse> {
        dataGrunnlag.bostedGrunnlag.finnBostedsadresse()?.let {
            return BostedsadresseTypeKode.BOSTEDSLAND to it.get()
        }
        dataGrunnlag.bostedGrunnlag.finnKontaktadresse()?.let {
            return BostedsadresseTypeKode.KONTAKTADRESSE to it.get()
        }
        throw FunksjonellException("Finner verken bostedsadresse eller kontaktadresse")
    }


    private fun hentAnmodningsperioder(): Collection<Anmodningsperiode> =
        anmodningsperiodeService.hentAnmodningsperioder(behandling.id).let { validerAnmodningsperioder(it) }

    private fun validerAnmodningsperioder(anmodningsperioder: Collection<Anmodningsperiode>): Collection<Anmodningsperiode> {
        if (anmodningsperioder.isEmpty()) {
            throw FunksjonellException("Minst en anmodningsperiode trengs for å kunne sende A001.")
        }
        anmodningsperioder.first().let { referansePeriode ->
            val lovvalgsperiodeIkkeGyldig = anmodningsperioder.any { !referansePeriode.gjelderSammeLandOgUnntakSom(it) }
            if (lovvalgsperiodeIkkeGyldig) {
                throw FunksjonellException("Flere anmodningsperioder støttes, men ikke med ulike land eller unntak.")
            }
        }
        return anmodningsperioder
    }

    private fun hentAnsettelsesperiode(): Periode? =
        behandling.finnArbeidsforholdDokument()?.let { arbeidsforholdDok ->
            val avklarteOrgnumre = dataGrunnlag.avklarteVirksomheterGrunnlag.hentNorskeArbeidsgivendeOrgnumre()
            arbeidsforholdDok.get().hentAnsettelsesperioder(avklarteOrgnumre).maxByOrNull { it.fom }
        } ?: throw TekniskException("Finner ikke arbeidsforhold")

}
