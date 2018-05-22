package no.nav.melosys.saksflyt.impl.agent;

import java.time.LocalDateTime;
import java.util.Optional;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Agent;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.impl.Utils;


public abstract class StandardAbstraktAgent implements Agent {

    private Binge binge;

    private ProsessinstansRepository prosessinstansRepo;

    public StandardAbstraktAgent(Binge binge, ProsessinstansRepository prosessinstansRepo) {
        this.binge = binge;
        this.prosessinstansRepo = prosessinstansRepo;
    }

    @Override
    /***
     * Metoden tar seg også av lagring i db og bingen.
     */
    public void finnProsessinstansOgUtførSteg() {
        Optional<Prosessinstans> opt = hentInstansMedSteg(inngangsSteg());
        if (opt.isPresent()) {
            Prosessinstans prosessinstans = opt.get();
            try {
                utførSteg(prosessinstans);
            } catch (RuntimeException e) {
                håndterFeil(prosessinstans, true);
            }
            prosessinstans.setSistEndret(LocalDateTime.now());
            prosessinstansRepo.save(prosessinstans);
            binge.leggTil(prosessinstans);
        }
    }

    void håndterFeil(Prosessinstans prosessinstans, boolean retry) {
        prosessinstans.setSteg(ProsessSteg.FEILET_MASKINELT);
        prosessinstans.setSistEndret(LocalDateTime.now());
        prosessinstansRepo.save(prosessinstans);
        // FIXME definere strategi
    }

    /**
     * Returnerer inngangssteget
     */
    public abstract ProsessSteg inngangsSteg();

    /**
     * Arbeidet som utføres av agenten. Metoden skal kunne kalles parallelt.
     */
    public abstract void utførSteg(Prosessinstans prosessinstans);

    private Optional<Prosessinstans> hentInstansMedSteg(ProsessSteg steg) {
        Prosessinstans prosessinstans = binge.fjernFørsteProsessinstans(Utils.medSteg(steg));
        return Optional.ofNullable(prosessinstans);
    }
}
