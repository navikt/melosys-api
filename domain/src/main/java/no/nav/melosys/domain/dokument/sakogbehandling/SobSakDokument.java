package no.nav.melosys.domain.dokument.sakogbehandling;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SobSakDokument extends SaksopplysningDokument {

    // Blir utledet av saksopplysningene
    private boolean eøsBarnetrygd;

    private String sakstema; // http://nav.no/kodeverk/Kodeverk/Sakstemaer

    private List<Behandlingskjede> behandlingskjede = new ArrayList<>();

    public boolean harEøsBarnetrygd() {
        return eøsBarnetrygd;
    }

    public void setEøsBarnetrygd(boolean eøsBarnetrygd) {
        this.eøsBarnetrygd = eøsBarnetrygd;
    }

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
