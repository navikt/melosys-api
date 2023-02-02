package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.domain.brev.utkast.UtkastBrev;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;

public record HentUtkastResponse(
    Long utkastBrevID,
    String lagretAvSaksbehandlerIdent,
    String tittel,
    BrevbestillingDto brevbestilling
) {

    public static HentUtkastResponse av(UtkastBrev utkastBrev) {
        return new HentUtkastResponse(
            utkastBrev.getId(),
            utkastBrev.getLagretAvSaksbehandler(),
            utkastBrev.getBrevbestillingUtkast().getTittel(),
            BrevbestillingDto.av(utkastBrev.getBrevbestillingUtkast())
        );
    }
}
