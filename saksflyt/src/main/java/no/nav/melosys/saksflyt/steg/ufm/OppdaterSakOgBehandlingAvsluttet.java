package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsService;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.steg.sob.SakOgBehandlingStegBehander;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSakOgBehandlingAvsluttet extends SakOgBehandlingStegBehander {

    private final TpsService tpsService;
    private final SaksopplysningRepository saksopplysningRepository;

    protected OppdaterSakOgBehandlingAvsluttet(SakOgBehandlingFasade sakOgBehandlingFasade, TpsService tpsService, SaksopplysningRepository saksopplysningRepository) {
        super(sakOgBehandlingFasade);
        this.tpsService = tpsService;
        this.saksopplysningRepository = saksopplysningRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_SAK_OG_BEHANDLING_AVSLUTTET;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {

        long behandlingId = prosessinstans.getBehandling().getId();
        String saksnummer = prosessinstans.getBehandling().getFagsak().getSaksnummer();
        String aktørId = prosessinstans.getData(ProsessDataKey.AKTØR_ID);

        if (aktørId == null) {
            aktørId = hentAktørIdFraTps(prosessinstans.getBehandling());
        }

        sakOgBehandlingAvsluttet(saksnummer, behandlingId, aktørId);
        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }

    private String hentAktørIdFraTps(Behandling behandling) throws TekniskException, IkkeFunnetException {
        Saksopplysning saksopplysning = saksopplysningRepository.findByBehandlingAndType(behandling, SaksopplysningType.PERSOPL)
            .orElseThrow(() -> new TekniskException("Personopplysninger ikke hentet for behandling " + behandling.getId()));

        PersonDokument personDokument = (PersonDokument) saksopplysning.getDokument();
        return tpsService.hentAktørIdForIdent(personDokument.fnr);
    }
}
