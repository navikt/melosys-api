package no.nav.melosys.tjenester.gui.dto.mottatteopplysninger;

import java.time.LocalDate;

import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper;

public class MottatteOpplysningerGetDto {

    private final MottatteOpplysningerData data;
    private final Mottatteopplysningertyper type;
    private final LocalDate mottaksdato;

    public MottatteOpplysningerGetDto(MottatteOpplysninger mottatteOpplysninger) {
        this.data = mottatteOpplysninger.getMottatteOpplysningerData();
        this.type = mottatteOpplysninger.getType();
        this.mottaksdato = mottatteOpplysninger.getMottaksdato();
    }

    public MottatteOpplysningerData getData() {
        return data;
    }

    public Mottatteopplysningertyper getType() {
        return type;
    }

    public LocalDate getMottaksdato() {
        return mottaksdato;
    }
}
