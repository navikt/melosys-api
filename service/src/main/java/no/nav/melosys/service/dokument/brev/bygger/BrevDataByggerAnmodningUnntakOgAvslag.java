package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.datagrunnlag.DokumentdataGrunnlag;

public class BrevDataByggerAnmodningUnntakOgAvslag implements BrevDataBygger {
    private final LandvelgerService landvelgerService;

    public BrevDataByggerAnmodningUnntakOgAvslag(LandvelgerService landvelgerService) {
        this.landvelgerService = landvelgerService;
    }

    @Override
    public BrevData lag(DokumentdataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag(saksbehandler);

        List<AvklartVirksomhet> avklarteVirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentAlleNorskeVirksomheterMedAdresse();
        if (avklarteVirksomheter.size() != 1) {
            throw new TekniskException("Trenger minst en norsk virksomhet for avslag og ART16.1");
        }

        brevData.hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        brevData.arbeidsland = landvelgerService.hentArbeidsland(dataGrunnlag.getBehandling()).getBeskrivelse();

        return brevData;
    }
}