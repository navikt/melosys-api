package no.nav.melosys.saksflyt.agent.unntak;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;

public class KjedetUnntakBehandler implements UnntakBehandler {

    private List<UnntakBehandler> kjede = new ArrayList<>();
    
    private KjedetUnntakBehandler() {
    }

    public static KjedetUnntakBehandler først(UnntakBehandler ub) {
        KjedetUnntakBehandler res = new KjedetUnntakBehandler();
        res.så(ub);
        return res;
    }
    
    public KjedetUnntakBehandler så(UnntakBehandler ub) {
        kjede.add(ub);
        return this;
    }

    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, Throwable t) {
        for (UnntakBehandler ub : kjede) {
            ub.behandleUnntak(prosessinstans, t);
        }
    }

}
