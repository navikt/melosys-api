package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektInformasjon;
import no.nav.melosys.domain.dokument.inntekt.ArbeidsInntektMaaned;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.exception.IkkeFunnetException;
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
import org.springframework.transaction.annotation.Transactional;

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
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException, IkkeFunnetException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = behandlingRepo.findOne(prosessinstans.getBehandling().getId());
        Set<String> orgnumre = new HashSet<>();
        Set<Saksopplysning> alleSaksopplysninger = behandling.getSaksopplysninger();

        Optional<Saksopplysning> arbeidsforholdSaksopplysning = hentSaksOpplysning(alleSaksopplysninger, SaksopplysningType.ARBEIDSFORHOLD);
        Optional<Saksopplysning> inntektSaksopplysning = hentSaksOpplysning(alleSaksopplysninger, SaksopplysningType.INNTEKT);

        arbeidsforholdSaksopplysning.ifPresent(saksopplysning -> orgnumre.addAll(hentOrgnumreFraArbeidsforhold(saksopplysning)));
        inntektSaksopplysning.ifPresent(saksopplysning -> orgnumre.addAll(hentOrgnumreFraInntekt(saksopplysning)));

        hentOrganisasjoner(orgnumre, behandling);

        prosessinstans.setSteg(ProsessSteg.HENT_MEDL_OPPL);
        log.info("Hentet organisasjonsopplysninger for prosessinstans {}", prosessinstans.getId());
    }

    private static Set<String> hentOrgnumreFraArbeidsforhold(Saksopplysning saksopplysning) {
        return ((ArbeidsforholdDokument) saksopplysning.getDokument()).getArbeidsforhold().stream()
            .flatMap(arbeidsforhold -> Stream.of(arbeidsforhold.getArbeidsgiverID(), arbeidsforhold.getOpplysningspliktigID()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private static Set<String> hentOrgnumreFraInntekt(Saksopplysning saksopplysning) {
        return ((InntektDokument) saksopplysning.getDokument()).getArbeidsInntektMaanedListe().stream()
            .map(ArbeidsInntektMaaned::getArbeidsInntektInformasjon)
            .filter(Objects::nonNull)
            .map(ArbeidsInntektInformasjon::getInntektListe)
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .map(Inntekt::getVirksomhetID)
            .collect(Collectors.toSet());
    }

    private Optional<Saksopplysning> hentSaksOpplysning(Set<Saksopplysning> saksopplysninger, SaksopplysningType saksopplysningType) {
        return saksopplysninger.stream().
            filter(saksopplysning -> saksopplysning.getType().equals(saksopplysningType)).findFirst();
    }

    private void hentOrganisasjoner(Set<String> orgnumre, Behandling behandling) throws SikkerhetsbegrensningException, IkkeFunnetException {
        for (String orgnr : orgnumre) {
            Saksopplysning saksopplysning = eregFasade.hentOrganisasjon(orgnr);
            saksopplysning.setBehandling(behandling);
            saksopplysning.setRegistrertDato(LocalDateTime.now());
            saksopplysningRepo.save(saksopplysning);
        }
    }

}
