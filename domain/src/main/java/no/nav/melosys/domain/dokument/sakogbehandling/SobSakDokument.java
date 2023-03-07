package no.nav.melosys.domain.dokument.sakogbehandling;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonView;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SobSakDokument implements SaksopplysningDokument {

    @JsonView(DokumentView.Database.class)
    private String sakstema; // http://nav.no/kodeverk/Kodeverk/Sakstemaer

    @JsonView(DokumentView.Database.class)
    private List<Behandlingskjede> behandlingskjede = new ArrayList<>();

    public String getSakstema() {
        return sakstema;
    }

    public void setSakstema(String sakstema) {
        this.sakstema = sakstema;
    }

    public List<Behandlingskjede> getBehandlingskjede() {
        return behandlingskjede;
    }

    public void setBehandlingskjede(List<Behandlingskjede> behandlingskjede) {
        this.behandlingskjede = behandlingskjede;
    }
}
