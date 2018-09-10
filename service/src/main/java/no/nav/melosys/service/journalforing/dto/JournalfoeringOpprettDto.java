package no.nav.melosys.service.journalforing.dto;

public class JournalfoeringOpprettDto extends JournalfoeringDto {
    private FagsakDto fagsak;

    public FagsakDto getFagsak() {
        return fagsak;
    }

    public void setFagsak(FagsakDto fagsak) {
        this.fagsak = fagsak;
    }
}
