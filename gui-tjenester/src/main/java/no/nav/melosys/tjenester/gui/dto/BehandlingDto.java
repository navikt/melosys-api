package no.nav.melosys.tjenester.gui.dto;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.tjenester.gui.jackson.serialize.SaksopplysningSerializer;

public class BehandlingDto {

    private long id;

    private BehandlingOppsummeringDto oppsummering;

    @JsonSerialize(using = SaksopplysningSerializer.class)
    private Set<SaksopplysningDokument> saksopplysninger;

    private Set<BehandlingHistorikkDto> behandlingshistorikk;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public BehandlingOppsummeringDto getOppsummering() {
        return oppsummering;
    }

    public void setOppsummering(BehandlingOppsummeringDto oppsummering) {
        this.oppsummering = oppsummering;
    }

    public Set<SaksopplysningDokument> getSaksopplysninger() {
        if (saksopplysninger == null) {
            saksopplysninger = new HashSet<>();
        }
        return saksopplysninger;
    }

    public void setSaksopplysninger(Set<SaksopplysningDokument> saksopplysninger) {
        this.saksopplysninger = saksopplysninger;
    }

    public Set<BehandlingHistorikkDto> getBehandlingshistorikk() {
        return behandlingshistorikk;
    }

    public void setBehandlingshistorikk(Set<BehandlingHistorikkDto> behandlingshistorikk) {
        this.behandlingshistorikk = behandlingshistorikk;
    }

}
