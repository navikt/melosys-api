package no.nav.melosys.service.dokument.brev;

import no.nav.foreldrepenger.integrasjon.dokument.felles.AvsenderAdresseType;
import no.nav.foreldrepenger.integrasjon.dokument.felles.KontaktInformasjonType;
import no.nav.melosys.domain.Behandling;

public final class BrevDataUtils {

    private BrevDataUtils() {

    }

    // FIXME
    static KontaktInformasjonType lageKontaktInformasjonType(Behandling behandling) {
        KontaktInformasjonType kontaktInformasjonType = new KontaktInformasjonType();
        kontaktInformasjonType.setKontaktTelefonnummer("KontaktTelefonnummer");
        //Adressen skal benyttes ved tilfeller der dokumenter må sendes i retur per post.
        AvsenderAdresseType avsenderadresse = new AvsenderAdresseType();
        avsenderadresse.setAdresselinje("setAdresselinje");
        avsenderadresse.setNavEnhetsNavn("setNavEnhetsNavn");
        avsenderadresse.setPostNr("7777");
        avsenderadresse.setPoststed("setPoststed");

        kontaktInformasjonType.setPostadresse(avsenderadresse);
        //Adressen skal benyttes dersom bruker/mottaker har behov for å kontakte NAV per post.
        kontaktInformasjonType.setReturadresse(avsenderadresse);
        return kontaktInformasjonType;
    }
}
