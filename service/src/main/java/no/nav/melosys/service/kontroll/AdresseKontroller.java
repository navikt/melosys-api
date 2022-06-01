package no.nav.melosys.service.kontroll;

import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.FysiskArbeidssted;
import org.apache.commons.lang.StringUtils;

import static org.apache.commons.lang.StringUtils.isBlank;

public final class AdresseKontroller {

    private AdresseKontroller() {
    }

    public static boolean manglerArbeidstedOpplysninger(FysiskArbeidssted arbeidssted) {
        return arbeidssted == null
            || manglerArbeidsstedVirksomhetsnavn(arbeidssted)
            || manglerArbeidsstedLandkode(arbeidssted);
    }

    public static boolean manglerArbeidsstedLandkode(FysiskArbeidssted arbeidssted) {
        return arbeidssted == null || arbeidssted.adresse == null || isBlank(arbeidssted.adresse.getLandkode());
    }

    public static boolean manglerArbeidsstedVirksomhetsnavn(FysiskArbeidssted arbeidssted) {
        return arbeidssted == null || isBlank(arbeidssted.virksomhetNavn);
    }

    public static boolean manglerForetakUtlandOpplysninger(ForetakUtland foretakUtland) {
        return foretakUtland == null
            || manglerForetakUtlandNavn(foretakUtland)
            || manglerForetakUtlandLandkode(foretakUtland);
    }

    public static boolean manglerForetakUtlandLandkode(ForetakUtland foretakUtland) {
        return StringUtils.isBlank(foretakUtland.adresse.getLandkode());
    }

    public static boolean manglerForetakUtlandNavn(ForetakUtland foretakUtland) {
        return foretakUtland == null || StringUtils.isBlank(foretakUtland.navn);
    }
}
