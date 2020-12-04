package no.nav.melosys.domain.familie;

import static no.nav.melosys.domain.familie.AvklarteMedfolgendeBarn.UUID_V4_PATTERN;

public class OmfattetBarn {

    public String fnr;
    public String uuid;
    public String sammensattNavn;

    public OmfattetBarn(String ident) {
        if (UUID_V4_PATTERN.matcher(ident).matches()) {
            uuid = ident;
        } else {
            fnr = ident;
        }
    }
}
