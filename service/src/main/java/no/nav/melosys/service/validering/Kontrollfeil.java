package no.nav.melosys.service.validering;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.validering.KontrollfeilDto;

import java.util.List;

public class Kontrollfeil {
    private Kontroll_begrunnelser kode;
    private List<String> felter;

    public Kontrollfeil(Kontroll_begrunnelser kode) {
        this.kode = kode;
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
