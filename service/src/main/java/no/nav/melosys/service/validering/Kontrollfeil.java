package no.nav.melosys.service.validering;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.validering.KontrollfeilDto;

public class Kontrollfeil {
    private final Kontroll_begrunnelser kode;
    private final List<String> felter;

    public Kontrollfeil(Kontroll_begrunnelser kode) {
        this.kode = kode;
        this.felter = Collections.emptyList();
    }

    public Kontrollfeil(Kontroll_begrunnelser kode, List<String> felter) {
        this.kode = kode;
        this.felter = felter;
    }

    public Kontroll_begrunnelser getKode() {
        return kode;
    }

    public List<String> getFelter() {
        return felter;
    }

    public KontrollfeilDto tilDto() {
        KontrollfeilDto dto = new KontrollfeilDto();
        dto.setKode(kode.getKode());
        dto.setFelter(felter);
        return dto;
    }
}
