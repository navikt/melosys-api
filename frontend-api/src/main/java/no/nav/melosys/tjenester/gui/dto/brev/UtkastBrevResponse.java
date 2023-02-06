package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.brev.utkast.UtkastBrev;

public record UtkastBrevResponse(
    Long utkastBrevID,
    String lagretAvSaksbehandlerIdent,
    String tittel,
    BrevbestillingRequest brevbestilling
) {

    public static UtkastBrevResponse av(UtkastBrev utkastBrev) {
        return new UtkastBrevResponse(
            utkastBrev.getId(),
            utkastBrev.getLagretAvSaksbehandler(),
            utkastBrev.getBrevbestillingUtkast().getTittel(),
            BrevbestillingRequest.av(utkastBrev.getBrevbestillingUtkast())
        );
    }
}
