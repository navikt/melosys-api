package no.nav.melosys.service;

import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektFasade;
import no.nav.melosys.integrasjon.medl.Medl2Fasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FagsakService {
    // TODO: Flytt henting og fletting av informasjon fra registre hit inntil videre

    private FagsakRepository fagsakRepository;

    private TpsFasade tpsFasade;

    private AaregFasade aaregFasade;

    private EregFasade eregFasade;

    private Medl2Fasade medl2Fasade;

    private InntektFasade inntektFasade;

    @Autowired
    public FagsakService(FagsakRepository fagsakRepository, TpsFasade tpsFasade, AaregFasade aaregFasade, EregFasade eregFasade, Medl2Fasade medl2Fasade, InntektFasade inntektFasade) {
        this.fagsakRepository = fagsakRepository;
        this.tpsFasade = tpsFasade;
        this.aaregFasade = aaregFasade;
        this.eregFasade = eregFasade;
        this.medl2Fasade = medl2Fasade;
        this.inntektFasade = inntektFasade;
    }
}
