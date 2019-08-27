package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;

public class BrevDataByggerAnmodningUnntakOgAvslag implements BrevDataBygger {

    private final Brevressurser brevressurser;

    public BrevDataByggerAnmodningUnntakOgAvslag(Brevressurser brevressurser) {
        this.brevressurser = brevressurser;
    }

    @Override
    public BrevData lag(String saksbehandler) throws FunksjonellException, TekniskException {
        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag(saksbehandler);

        List<AvklartVirksomhet> avklarteVirksomheter = brevressurser.getAvklarteVirksomheter().hentAlleNorskeVirksomheterMedAdresse();
        if (avklarteVirksomheter.size() != 1) {
            throw new TekniskException("Trenger minst en norsk virksomhet for avslag og ART16.1");
        }

        brevData.hovedvirksomhet = brevressurser.getAvklarteVirksomheter().hentHovedvirksomhet();
        brevData.arbeidsland = brevressurser.getLandvelger().hentArbeidsland(brevressurser.getBehandling()).getBeskrivelse();

        return brevData;
    }
}