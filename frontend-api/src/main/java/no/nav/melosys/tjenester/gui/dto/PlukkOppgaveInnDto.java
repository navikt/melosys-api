package no.nav.melosys.tjenester.gui.dto;

        import java.util.List;

public class PlukkOppgaveInnDto {

    private List<String> sakstyper;
    private List<String> behandlingstyper;

    public List<String> getSakstyper() {
        return sakstyper;
    }

    public void setSakstyper(List<String> sakstyper) {
        this.sakstyper = sakstyper;
    }

    public List<String> getBehandlingstyper() {
        return behandlingstyper;
    }

    public void setBehandlingstyper(List<String> behandlingstyper) {
        this.behandlingstyper = behandlingstyper;
    }

}
