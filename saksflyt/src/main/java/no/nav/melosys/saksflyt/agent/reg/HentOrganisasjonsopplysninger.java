package no.nav.melosys.saksflyt.agent.reg;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Steget sørger for å hente Opplysninger om Orgnisjoner fra EREG
 *
 * Transisjoner:
 * HENT_ORG_OPP → HENT_MEDL_OPPL hvis alt ok
 */
@Component
public class HentOrganisasjonsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentOrganisasjonsopplysninger.class);

    private final BehandlingRepository behandlingRepo;
    private final SaksopplysningRepository saksopplysningRepo;
    private final EregFasade eregFasade;

    @Autowired
    public HentOrganisasjonsopplysninger(BehandlingRepository behandlingRepo, SaksopplysningRepository saksopplysningRepo, @Qualifier("system")EregFasade eregFasade) {
        this.behandlingRepo = behandlingRepo;
        this.saksopplysningRepo = saksopplysningRepo;
        this.eregFasade = eregFasade;
        log.info("HentOrganisasjonsopplysninger initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_ORG_OPPL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = behandlingRepo.findWithSaksopplysningerById(prosessinstans.getBehandling().getId());
        Set<String> orgnumre = new HashSet<>();

        Optional<SaksopplysningDokument> arbeidsforholdDokument = SaksopplysningerUtils.hentDokument(behandling, SaksopplysningType.ARBFORH);
        Optional<SaksopplysningDokument> inntektDokument = SaksopplysningerUtils.hentDokument(behandling, SaksopplysningType.INNTK);

        arbeidsforholdDokument.ifPresent(dokument -> orgnumre.addAll(((ArbeidsforholdDokument)dokument).hentOrgnumre()));
        inntektDokument.ifPresent(dokument -> orgnumre.addAll(((InntektDokument)dokument).hentOrgnumre()));

        hentOgLagreOrganisasjoner(behandling, orgnumre);

        prosessinstans.setSteg(ProsessSteg.HENT_MEDL_OPPL);
        log.info("Hentet organisasjonsopplysninger for prosessinstans {}", prosessinstans.getId());
    }

    private void hentOgLagreOrganisasjoner(Behandling behandling, Set<String> orgnumre) throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {

        for (String orgnr : orgnumre) {
            Instant nå = Instant.now();
            Saksopplysning saksopplysning = eregFasade.hentOrganisasjon(orgnr);
            saksopplysning.setBehandling(behandling);
            saksopplysning.setRegistrertDato(nå);
            saksopplysning.setEndretDato(nå);
            saksopplysningRepo.save(saksopplysning);
        }
    }

}
