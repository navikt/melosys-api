package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.brev.OrienteringTilArbeidsgiverOmVedtakBrevbestilling
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.integrasjon.dokgen.dto.OrienteringTilArbeidsgiverOmVedtak
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import org.springframework.stereotype.Component

@Component
class OrienteringTilArbeidsgiverOmVedtakMapper(
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val vilkaarsresultatService: VilkaarsresultatService,
    private val avklartefaktaService: AvklartefaktaService,
    private val virksomheterService: AvklarteVirksomheterService,
    private val landvelgerService: LandvelgerService
) {
    @Transactional
    internal fun map(brevbestilling: OrienteringTilArbeidsgiverOmVedtakBrevbestilling): OrienteringTilArbeidsgiverOmVedtak {
        val behandlingsresultat = dokgenMapperDatahenter.hentBehandlingsresultat(brevbestilling.behandlingId)
        val behandlingID = behandlingsresultat.behandling.id
        val lovvalgsperiode = behandlingsresultat.hentLovvalgsperiode()
        val periodeFom = lovvalgsperiode.fom
        val periodeTom = lovvalgsperiode.tom
        val arbeidsland = landvelgerService.hentArbeidsland(behandlingID).beskrivelse

        val alleAvklarteOrgnr = avklartefaktaService.hentAvklarteOrgnrOgUuid(behandlingsresultat.id)
        val alleVirksomheter = virksomheterService.hentAlleNorskeVirksomheter(behandlingsresultat.behandling)

        val navnVirksomhet = alleVirksomheter.stream()
            .filter { alleAvklarteOrgnr.contains(it.orgnr) }
            .findFirst().get().navn
        val vilkaarResultat = vilkaarsresultatService.finnVilkaarsresultat(behandlingID, Vilkaar.VESENTLIG_VIRKSOMHET)



        val erInnvilgelse = brevbestilling.erInnvilgelse || behandlingsresultat.erInnvilgelse()
        var erVesentligVirksomhetOppfyllt = false
        var vesentligVirksomhetBegrunnelser = listOf<String>()

        if(vilkaarResultat != null) {
            erVesentligVirksomhetOppfyllt = vilkaarResultat.isOppfylt
            vesentligVirksomhetBegrunnelser = vilkaarResultat.begrunnelser.stream().map { it.kode }.toList()
        }

        return OrienteringTilArbeidsgiverOmVedtak(
            brevbestilling,
            periodeFom,
            periodeTom,
            arbeidsland,
            erInnvilgelse,
            erVesentligVirksomhetOppfyllt,
            navnVirksomhet,
            vesentligVirksomhetBegrunnelser
        )
    }
}
