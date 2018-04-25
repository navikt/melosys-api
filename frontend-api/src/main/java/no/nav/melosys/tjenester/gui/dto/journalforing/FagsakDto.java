package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.LocalDateTime;

import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;

public class FagsakDto {
    private Long saksnummer;
    private LocalDateTime registrertDato;
    private FagsakType type;
    private FagsakStatus status;
    private PeriodeDto soknadsperiode;
    private KodeDto landkode; //TODO er det en liste?

    public Long getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Long saksnummer) {
        this.saksnummer = saksnummer;
    }

    public LocalDateTime getRegistrertDato() {
        return registrertDato;
    }

    public void setRegistrertDato(LocalDateTime registrertDato) {
        this.registrertDato = registrertDato;
    }

    public FagsakType getType() {
        return type;
    }

    public void setType(FagsakType type) {
        this.type = type;
    }

    public FagsakStatus getStatus() {
        return status;
    }

    public void setStatus(FagsakStatus status) {
        this.status = status;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }

    public KodeDto getLandkode() {
        return landkode;
    }

    public void setLandkode(KodeDto landkode) {
        this.landkode = landkode;
    }
}
