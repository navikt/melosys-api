package no.nav.melosys.service.validering;

import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.validering.KontrollfeilDto;
import no.nav.melosys.exception.KontrolldataFeilType;

public class Kontrollfeil {
    private final Kontroll_begrunnelser kode;
    private final List<String> felter;
    private final KontrolldataFeilType type;

    public Kontrollfeil(Kontroll_begrunnelser kode) {
        this.kode = kode;
        this.type = KontrolldataFeilType.FEIL;
        this.felter = Collections.emptyList();
    }

    public Kontrollfeil(Kontroll_begrunnelser kode, KontrolldataFeilType type) {
        this.kode = kode;
        this.type = type;
        this.felter = Collections.emptyList();
    }

    public Kontrollfeil(Kontroll_begrunnelser kode, List<String> felter, KontrolldataFeilType type) {
        this.kode = kode;
        this.felter = felter;
        this.type = type;
    }

    public Kontroll_begrunnelser getKode() {
        return kode;
    }

    public KontrolldataFeilType getType() {
        return type;
    }

    public List<String> getFelter() {
        return felter;
    }

    public KontrollfeilDto tilDto() {
        KontrollfeilDto dto = new KontrollfeilDto();
        dto.setKode(kode.getKode());
        dto.setFelter(felter);
        dto.setType(type);
        return dto;
    }

    @Override
    public String toString() {
        return String.format("%s %s", kode, felter);
    }
}
