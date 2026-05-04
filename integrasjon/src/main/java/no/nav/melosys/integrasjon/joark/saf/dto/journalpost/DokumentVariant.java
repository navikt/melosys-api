package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record DokumentVariant(boolean saksbehandlerHarTilgang, String variantformat, String filtype) {

    private static final Logger log = LoggerFactory.getLogger(DokumentVariant.class);

    public no.nav.melosys.domain.arkiv.DokumentVariant tilDomene() {
        return new no.nav.melosys.domain.arkiv.DokumentVariant(
            null,
            tilDomeneFiltype(filtype),
            no.nav.melosys.domain.arkiv.DokumentVariant.VariantFormat.valueOf(variantformat),
            saksbehandlerHarTilgang
        );
    }

    private static no.nav.melosys.domain.arkiv.DokumentVariant.Filtype tilDomeneFiltype(String filtype) {
        if (filtype == null) return null;
        try {
            return no.nav.melosys.domain.arkiv.DokumentVariant.Filtype.valueOf(filtype);
        } catch (IllegalArgumentException e) {
            log.warn("Ukjent filtype '{}' fra SAF, mapper til null", filtype);
            return null;
        }
    }
}
