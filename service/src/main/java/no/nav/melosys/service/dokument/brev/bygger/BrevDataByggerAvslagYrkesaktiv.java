package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;

public class BrevDataByggerAvslagYrkesaktiv implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerAvslagYrkesaktiv(LandvelgerService landvelgerService,
                                          AnmodningsperiodeService anmodningsperiodeService,
                                          VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.landvelgerService = landvelgerService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataAvslagYrkesaktiv brevData = new BrevDataAvslagYrkesaktiv(saksbehandler);
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

        return brevData;
    }

    private Optional<AnmodningsperiodeSvar> hentAnmodningsperiodeSvar(long behandlingID) {
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.hentAnmodningsperioder(behandlingID);
        return anmodningsperioder.stream()
            .filter(Anmodningsperiode::erSendtUtland)
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar);
    }

    private Vilkaarsresultat hentFørsteGyldigeVilkaarsresultatForArt16(long behandlingID) throws FunksjonellException {
        return vilkaarsresultatRepository.findByBehandlingsresultatIdAndVilkaar(behandlingID, FO_883_2004_ART16_1)
            .filter(v -> !v.getBegrunnelser().isEmpty()).orElseThrow(() -> new FunksjonellException("Avslag yrkesaktiv må ha vilkår for art16"));
    }
}