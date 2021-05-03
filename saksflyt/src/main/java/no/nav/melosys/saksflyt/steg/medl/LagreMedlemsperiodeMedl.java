package no.nav.melosys.saksflyt.steg.medl;

import java.util.Collection;

import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Component
public class LagreMedlemsperiodeMedl implements StegBehandler {

    private final MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public LagreMedlemsperiodeMedl(MedlemAvFolketrygdenService medlemAvFolketrygdenService, MedlPeriodeService medlPeriodeService) {
        this.medlemAvFolketrygdenService = medlemAvFolketrygdenService;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.LAGRE_MEDLEMSKAPSPERIODE_MEDL;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        long behandlingId = prosessinstans.getBehandling().getId();
        MedlemAvFolketrygden medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingId);
        Collection<Medlemskapsperiode> medlemskapsperioder = medlemAvFolketrygden.getMedlemskapsperioder();

        if (medlemskapsperioder.isEmpty()) {
            throw new FunksjonellException("Ingen medlemskapsperioder funnet for behandling " + behandlingId);
        }

        for (Medlemskapsperiode medlemskapsperiode : medlemskapsperioder) {
            long medlPeriodeId = opprettMedlPeriode(behandlingId, medlemskapsperiode);
            medlemskapsperiode.setMedlPeriodeID(medlPeriodeId);
        }

        medlemAvFolketrygdenService.lagreMedlemAvFolketrygden(medlemAvFolketrygden);
    }

    private long opprettMedlPeriode(long behandlingId, Medlemskapsperiode medlemskapsperiode) throws FunksjonellException {
        return ofNullable(medlPeriodeService.opprettPeriodeEndelig(behandlingId, medlemskapsperiode))
            .orElseThrow(() -> new FunksjonellException(format("Oppretting av medlemskapsperiode %s i MEDL feilet for behandling %s", medlemskapsperiode.getId(), behandlingId)));
    }
}
