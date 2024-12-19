package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.Unleash
import no.nav.melosys.domain.brev.InnvilgelseEftaStorbritanniaBrevbestilling
import no.nav.melosys.domain.dokument.felles.Periode
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.dokgen.dto.InnvilgelseEftaStorbritannia
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Component
class InnvilgelseEftaStorbritanniaMapper(
    private val vilkaarsresultatService: VilkaarsresultatService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val virksomheterService: AvklarteVirksomheterService,
    private val avklartefaktaService: AvklartefaktaService,
    private val landvelgerService: LandvelgerService,
    private val unleash: Unleash
) {
    @Transactional(readOnly = true)
    internal fun mapInnvilgelseEftaStorbritannia(brevbestilling: InnvilgelseEftaStorbritanniaBrevbestilling): InnvilgelseEftaStorbritannia {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val lovvalgsperiode = behandlingsresultat.hentLovvalgsperiode()
        val anmodningsperiode = behandlingsresultat.finnAnmodningsperiode()
        val erNorskSkip = vilkaarsresultatService.finnVilkaarsresultat(behandlingsresultat.id, Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP)
        val erUnntakTuristskip = vilkaarsresultatService.oppfyllerVilkaar(behandlingsresultat.id, Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP)
        val bostedsland = landvelgerService.hentBostedsland(behandlingsresultat.behandling).landkodeobjekt
        val sedAvsenderlandKode = behandlingsresultat.behandling.finnSedDokument().getOrNull()?.avsenderLandkode
        val søknadsland = behandlingsresultat.behandling.finnMottatteOpplysningerData().getOrNull()?.soeknadsland
        val er11_3_a_og_flereArbeidsland = (søknadsland?.landkoder?.size ?: 0) > 1 && lovvalgsperiode.erArtikkel11_3_a()

        val alleVirksomheterNorge = virksomheterService.hentAlleNorskeVirksomheter(behandlingsresultat.behandling)
        val alleVirksomheterUtlandet = virksomheterService.hentUtenlandskeVirksomheter(behandlingsresultat.behandling)
        val alleVirksomheter = alleVirksomheterNorge + alleVirksomheterUtlandet

        val navnVirksomheter = alleVirksomheter.stream().map { it.navn }.toList()

        val erOrdinaerYrkesgruppe =
            behandlingsresultat.avklartefakta.find { it.type == Avklartefaktatyper.YRKESGRUPPE && it.fakta == Yrkesgrupper.ORDINAER.name } != null

        val arbeidINorge =
            if (unleash.isEnabled(ToggleName.MELOSYS_ARBEID_KUN_NORGE)) bostedsland.kode == Land_iso2.NO.name && erOrdinaerYrkesgruppe else false

        val er11_3_a_eller_13_a_arbeid_norge = if (arbeidINorge) {
            lovvalgsperiode.erArtikkel11_3_a_eller_13_3a()
        } else {
            lovvalgsperiode.erArtikkel11_3_a() && behandlingsresultat.behandling.erNorgeUtpekt() && !er11_3_a_og_flereArbeidsland && unleash.isEnabled(
                ToggleName.MELOSYS_11_3_A_NORGE_ER_UTPEKT
            )
        }

        return InnvilgelseEftaStorbritannia(
            brevbestilling = brevbestilling,
            navnVirksomheter = navnVirksomheter,
            behandlingstype = behandlingsresultat.behandling.type,
            behandlingstema = behandlingsresultat.behandling.tema,
            sedAvsenderlandKode?.beskrivelse,
            nyVurderingBakgrunn = brevbestilling.nyVurderingBakgrunn,
            lovvalgsbestemmelse = lovvalgsperiode.bestemmelse.name(),
            erUnntakTuristskip = erUnntakTuristskip,
            erNorskSkip = erNorskSkip != null,
            lovvalgsperiode = Periode(lovvalgsperiode.fom, lovvalgsperiode.tom),
            tilleggsbestemmelse = if (lovvalgsperiode.tilleggsbestemmelse != null) lovvalgsperiode.tilleggsbestemmelse.name() else "",
            erArtikkel11_3_a_eller_13_3_a_arbeid_norge = er11_3_a_eller_13_a_arbeid_norge,
            erArtikkel13_3_a_eller_13_4 = if (!arbeidINorge) lovvalgsperiode.erArtikkel13_3_a_eller_13_4() else false,
            erArtikkel14_1_eller_14_2 = lovvalgsperiode.erArtikkel14_1_eller_14_2(),
            erArtikkel16_1_eller_16_3 = lovvalgsperiode.erArtikkel16_1_eller_16_3(),
            erArtikkel18_1 = lovvalgsperiode.erArtikkel18_1(),
            bosted = bostedsland.beskrivelse,
            anmodningsperiodeSvarType = if (anmodningsperiode.isPresent) anmodningsperiode.get().anmodningsperiodeSvar.anmodningsperiodeSvarType.name else "",
            innvilgelseFritekst = brevbestilling.innvilgelseFritekst,
            innledningFritekst = brevbestilling.innledningFritekst,
            begrunnelseFritekst = if (brevbestilling.begrunnelseFritekst.isNullOrEmpty()) behandlingsresultat.begrunnelseFritekst else brevbestilling.begrunnelseFritekst,
            erArtikkel11_3_a_og_flereArbeidsland = er11_3_a_og_flereArbeidsland
        )
    }
}
