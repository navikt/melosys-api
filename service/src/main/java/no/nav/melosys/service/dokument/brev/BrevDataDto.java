package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.RolleType;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

public class BrevDataDto {

    public String saksbehandler;

    public RolleType mottaker;

    public String fritekst;

    public BrevDataDto() {
        saksbehandler = SubjectHandler.getInstance().getUserID();
    }
}
