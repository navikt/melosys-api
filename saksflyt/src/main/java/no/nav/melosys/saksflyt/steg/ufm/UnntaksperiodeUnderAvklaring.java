package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.saksflyt.felles.OppdaterMedlFelles;
import no.nav.melosys.saksflyt.felles.UnntaksperiodeUtils;
import no.nav.melosys.service.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UnntaksperiodeUnderAvklaring extends AbstraktStegBehandler {

    private final OppdaterMedlFelles felles;
    private final MedlFasade medlFasade;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public UnntaksperiodeUnderAvklaring(OppdaterMedlFelles felles, MedlFasade medlFasade, BehandlingService behandlingService, BehandlingsresultatRepository behandlingsresultatRepository) {
        this.felles = felles;
        this.medlFasade = medlFasade;
        this.behandlingService = behandlingService;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_UNDER_AVKLARING;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        final long behandlingId = prosessinstans.getBehandling().getId();
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandlingId)
            .orElseThrow(() -> new TekniskException("Behandlingsresultat ikke funnet for behandling" + behandlingId));

        Set<Lovvalgsperiode> lovvalgsperioder = behandlingsresultat.getLovvalgsperioder();
        if (lovvalgsperioder.isEmpty()) {
            Behandling behandling = behandlingService.hentBehandling(behandlingId);
            SedDokument sedDokument = SaksopplysningerUtils.hentSedDokument(behandling);
            PersonDokument personDokument = SaksopplysningerUtils.hentPersonDokument(behandling);
            Lovvalgsperiode lovvalgsperiode = UnntaksperiodeUtils.opprettLovvalgsperiode(sedDokument);
            Long medlperiodeId = medlFasade.opprettPeriodeUnderAvklaring(personDokument.fnr, lovvalgsperiode, KildedokumenttypeMedl.SED);
            felles.lagreMedlPeriodeId(medlperiodeId, lovvalgsperiode, behandlingId);
        }

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
