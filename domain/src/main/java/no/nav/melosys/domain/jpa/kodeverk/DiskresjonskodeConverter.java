package no.nav.melosys.domain.jpa.kodeverk;

import no.nav.melosys.domain.dokument.person.Diskresjonskode;

public class DiskresjonskodeConverter extends AbstraktKodeverkHjelperConverter<Diskresjonskode> {

    @Override
    public Diskresjonskode convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        Diskresjonskode diskresjonskode = new Diskresjonskode();
        diskresjonskode.setKode(s);
        return diskresjonskode;
    }
}
