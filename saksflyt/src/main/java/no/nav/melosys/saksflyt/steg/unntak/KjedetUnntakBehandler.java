package no.nav.melosys.saksflyt.steg.unntak;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;

public class KjedetUnntakBehandler implements UnntakBehandler {

    private List<UnntakBehandler> kjede = new ArrayList<>();
    
    private KjedetUnntakBehandler() {
    }

    public static KjedetUnntakBehandler først(UnntakBehandler ub) {
        KjedetUnntakBehandler res = new KjedetUnntakBehandler();
        return res.så(ub);
    }
    
    public KjedetUnntakBehandler så(UnntakBehandler ub) {
        kjede.add(ub);
        return this;
    }

    @Override
    public void behandleUnntak(Prosessinstans prosessinstans, String melding, Throwable t) {
        for (UnntakBehandler ub : kjede) {
            ub.behandleUnntak(prosessinstans, melding, t);
        }
    }

}
