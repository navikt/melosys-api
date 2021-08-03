package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART16_1;

public class BrevDataByggerAvslagYrkesaktiv implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BrevbestillingRequest brevbestilling;
    private final VilkaarsresultatService vilkaarsresultatService;

    public BrevDataByggerAvslagYrkesaktiv(LandvelgerService landvelgerService,
                                          AnmodningsperiodeService anmodningsperiodeService,
                                          BrevbestillingRequest brebestilling,
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
            throw new TekniskException("Ingen eller flere enn én norsk eller utenlandsk virksomhet oppgitt for avslag eller ART16.1");
        }

        brevData.hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        brevData.yrkesaktivitet = brevData.hovedvirksomhet.yrkesaktivitet;
        brevData.arbeidsland = landvelgerService.hentArbeidsland(behandlingID).getBeskrivelse();
        brevData.art16Vilkaar = hentFørsteGyldigeVilkaarsresultatForArt16(behandlingID);

        if (!brevData.art16Vilkaar.isOppfylt()) {
            brevData.anmodningsperiodeSvar = Optional.empty();
        } else {
            brevData.anmodningsperiodeSvar = hentAnmodningsperiodeSvar(behandlingID);
        }

        brevData.erArt16UtenArt12 = vilkaarsresultatService.harVilkaarForArtikkel16(behandlingID) && !vilkaarsresultatService.harVilkaarForArtikkel12(behandlingID);

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
