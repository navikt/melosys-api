package no.nav.melosys.service.dokument.brev.ressurser;

import java.util.Collection;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;

public class LovvalgsperiodeRessurs {
    private final Behandling behandling;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public LovvalgsperiodeRessurs(Behandling behandling, LovvalgsperiodeService lovvalgsperiodeService) {
        this.behandling = behandling;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    public Collection<Lovvalgsperiode> hentLovvalgsperioder() throws TekniskException {
        Collection<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeService.hentLovvalgsperioder(behandling.getId());
        if (lovvalgsperioder.isEmpty()) {
            throw new TekniskException("Trenger minst en lovvalgsperiode");
        }
        return lovvalgsperioder;
    }

    public Lovvalgsperiode hentLovvalgsperiode() throws FunksjonellException, TekniskException {
        Collection<Lovvalgsperiode> lovvalgsperioder = hentLovvalgsperioder();
        if (lovvalgsperioder.size() > 1) {
            throw new FunksjonellException("Forventer kun en lovvalgsperiode!");
        }
        return lovvalgsperioder.iterator().next();
    }
}
