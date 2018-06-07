package no.nav.melosys.dokument.innvilget;

import no.nav.foreldrepenger.integrasjon.dokument.innvilget.BrevdataType;

// FIXME Fjernes når vi får xsd for Melosys i dokprod
public final class InnvilgetConstants {

    public static final String NAMESPACE = "urn:no.nav.foreldrepenger.integrasjon.dokument.innvilget.v1";
    public static final String XSD_LOCATION = "xsd/foreldrepenger_000048.xsd";
    public static final Class<BrevdataType> JAXB_CLASS = BrevdataType.class;

    private InnvilgetConstants() {
    }
}
