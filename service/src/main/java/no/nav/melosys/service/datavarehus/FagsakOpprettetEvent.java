package no.nav.melosys.service.datavarehus;

import no.nav.melosys.domain.Fagsak;

public class FagsakOpprettetEvent {

    public Fagsak fagsak;

    public String endretAv;

    public FagsakOpprettetEvent(Fagsak fagsak, String endretAv) {
        this.fagsak = fagsak;
        this.endretAv = endretAv;
    }
}
