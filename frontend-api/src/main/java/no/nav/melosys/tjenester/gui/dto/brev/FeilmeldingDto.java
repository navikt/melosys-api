package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;

public record FeilmeldingDto(String tittel, Collection<FeilmeldingUnderpunkt> underpunkter) {

    public FeilmeldingDto(Kontroll_begrunnelser begrunnelse, String underpunkt) {
        this(begrunnelse.getBeskrivelse(), List.of(new FeilmeldingUnderpunkt(underpunkt)));
    }

    public FeilmeldingDto(Kontroll_begrunnelser begrunnelse) {
        this(begrunnelse.getBeskrivelse(), Collections.emptyList());

    }

    public FeilmeldingDto(String tittel) {
        this(tittel, Collections.emptyList());
    }

}

