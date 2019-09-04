package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;

import static no.nav.melosys.domain.kodeverk.Vilkaar.FO_883_2004_ART16_1;

public class BrevDataByggerAnmodningUnntakOgAvslag implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    public BrevDataByggerAnmodningUnntakOgAvslag(LandvelgerService landvelgerService,
                                                 AnmodningsperiodeService anmodningsperiodeService,
                                                 VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.landvelgerService = landvelgerService;
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    @Override
    public BrevData lag(DokumentdataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag(saksbehandler);
        Behandling behandling = dataGrunnlag.getBehandling();
        List<AvklartVirksomhet> avklarteVirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentAlleNorskeVirksomheterMedAdresse();
        if (avklarteVirksomheter.size() != 1) {
            throw new TekniskException("Trenger minst en norsk virksomhet for avslag og ART16.1");
        }

        brevData.hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        brevData.yrkesaktivitet = brevData.hovedvirksomhet.yrkesaktivitet;
        brevData.arbeidsland = landvelgerService.hentArbeidsland(behandling).getBeskrivelse();
        brevData.art16Vilkaar = hentFørsteGyldigeVilkaarsresultatForArt16(behandling);

        brevData.anmodningsperiodeSvar = hentAnmodningsperiodeSvar(behandling);

        return brevData;
    }

    private Optional<AnmodningsperiodeSvar> hentAnmodningsperiodeSvar(Behandling behandling) {
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.hentAnmodningsperioder(behandling.getId());
        return anmodningsperioder.stream()
            .filter(Anmodningsperiode::erSendtUtland)
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar);
    }

    private Optional<Vilkaarsresultat> hentFørsteGyldigeVilkaarsresultatForArt16(Behandling behandling) {
        return vilkaarsresultatRepository.findByBehandlingsresultatId(behandling.getId()).stream()
            .filter(v -> v.getVilkaar() == FO_883_2004_ART16_1 && !v.getBegrunnelser().isEmpty())
            .findFirst();
    }
}