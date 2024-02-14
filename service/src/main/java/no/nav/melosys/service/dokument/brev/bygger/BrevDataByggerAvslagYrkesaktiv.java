package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART16_1;

public class BrevDataByggerAvslagYrkesaktiv implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BrevbestillingDto brevbestilling;
    private final VilkaarsresultatService vilkaarsresultatService;

    public BrevDataByggerAvslagYrkesaktiv(LandvelgerService landvelgerService,
                                          AnmodningsperiodeService anmodningsperiodeService,
                                          BrevbestillingDto brebestilling,
                                          VilkaarsresultatService vilkaarsresultatService) {
        this.landvelgerService = landvelgerService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.brevbestilling = brebestilling;
        this.vilkaarsresultatService = vilkaarsresultatService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        BrevDataAvslagYrkesaktiv brevData = new BrevDataAvslagYrkesaktiv(brevbestilling, saksbehandler);
        long behandlingID = dataGrunnlag.getBehandling().getId();
        if (dataGrunnlag.getAvklarteVirksomheterGrunnlag().antallVirksomheter() != 1) {
            throw new TekniskException(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET.getBeskrivelse());
        }

        brevData.setHovedvirksomhet(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet());
        brevData.setYrkesaktivitet(brevData.getHovedvirksomhet().yrkesaktivitet);
        brevData.setArbeidsland(landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse());
        brevData.setArt16Vilkaar(hentFørsteGyldigeVilkaarsresultatForArt16(behandlingID));

        if (!brevData.getArt16Vilkaar().isOppfylt()) {
            brevData.setAnmodningsperiodeSvar(Optional.empty());
        } else {
            brevData.setAnmodningsperiodeSvar(hentAnmodningsperiodeSvar(behandlingID));
        }

        brevData.setArt16UtenArt12(vilkaarsresultatService.harVilkaarForArtikkel16(behandlingID) && !vilkaarsresultatService.harVilkaarForArtikkel12(behandlingID));

        return brevData;
    }

    private Optional<AnmodningsperiodeSvar> hentAnmodningsperiodeSvar(long behandlingID) {
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.hentAnmodningsperioder(behandlingID);
        return anmodningsperioder.stream()
            .filter(Anmodningsperiode::erSendtUtland)
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar);
    }

    private Vilkaarsresultat hentFørsteGyldigeVilkaarsresultatForArt16(long behandlingID) {
        return vilkaarsresultatService.finnVilkaarsresultat(behandlingID, FO_883_2004_ART16_1)
            .filter(v -> !v.getBegrunnelser().isEmpty()).orElseThrow(() -> new FunksjonellException("Avslag yrkesaktiv må ha vilkår for art16"));
    }
}
