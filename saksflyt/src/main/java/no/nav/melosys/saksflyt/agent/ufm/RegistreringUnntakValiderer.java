package no.nav.melosys.saksflyt.agent.ufm;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RegistreringUnntakValiderer extends AbstraktStegBehandler {

    protected final SaksopplysningRepository saksopplysningRepository;
    private final AvklartefaktaService avklartefaktaService;

    @Autowired
    RegistreringUnntakValiderer(SaksopplysningRepository saksopplysningRepository, AvklartefaktaService avklartefaktaService) {
        this.saksopplysningRepository = saksopplysningRepository;
        this.avklartefaktaService = avklartefaktaService;
    }

    void registrerFeil(Prosessinstans prosessinstans, Unntak_periode_begrunnelser treffBegrunnelse) throws IkkeFunnetException {

        avklartefaktaService.leggTilAvklarteFakta(prosessinstans.getBehandling().getId(),
            Avklartefaktatype.VURDERING_UNNTAK_PERIODE, null, null, treffBegrunnelse.getKode());
    }

    Saksopplysning hentSedSaksopplysning(Prosessinstans prosessinstans) throws TekniskException {
        return saksopplysningRepository.findByBehandlingAndType(prosessinstans.getBehandling(), SaksopplysningType.SED_OPPLYSNINGER)
            .orElseThrow(() -> new TekniskException("Seddokument finnes ikke for behandling " + prosessinstans.getBehandling().getId()));
    }
}
