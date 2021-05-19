package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;

public interface BrevDataBygger {

    BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler);
}
