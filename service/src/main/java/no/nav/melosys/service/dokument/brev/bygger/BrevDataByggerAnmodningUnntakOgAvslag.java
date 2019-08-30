package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;

public class BrevDataByggerAnmodningUnntakOgAvslag implements BrevDataBygger {
    private final LandvelgerService landvelgerService;
    private final AnmodningsperiodeService anmodningsperiodeService;

    public BrevDataByggerAnmodningUnntakOgAvslag(LandvelgerService landvelgerService,
                                                 AnmodningsperiodeService anmodningsperiodeService) {
        this.landvelgerService = landvelgerService;
        this.anmodningsperiodeService = anmodningsperiodeService;
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
        brevData.arbeidsland = landvelgerService.hentArbeidsland(behandling).getBeskrivelse();

        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.hentAnmodningsperioder(behandling.getId());
        brevData.anmodningsperiodeSvar = anmodningsperioder.stream()
            .findFirst()
            .map(Anmodningsperiode::getAnmodningsperiodeSvar);

        return brevData;
    }
}