package no.nav.melosys.saksflyt.agent.reg;

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
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Steget sørger for å hente Opplysninger om Orgnisjoner fra EREG
 * <p>
 * Transisjoner:
 * HENT_ORG_OPP → HENT_MEDL_OPPL hvis alt ok
 */
@Component
public class HentOrganisasjonsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentOrganisasjonsopplysninger.class);

    private final FagsakService fagsakService;

    private final EregFasade eregFasade;

    @Autowired
    public HentOrganisasjonsopplysninger(FagsakService fagsakService, EregFasade eregFasade) {
        this.fagsakService = fagsakService;
        this.eregFasade = eregFasade;
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
    public void utførSteg(Prosessinstans prosessinstans) {

        try {
            Set<String> orgnumre = new HashSet<>();
            Set<Saksopplysning> alleSaksopplysninger = prosessinstans.getBehandling().getSaksopplysninger();

            Optional<Saksopplysning> arbeidsforholdSaksopplysning = hentSaksOpplysning(alleSaksopplysninger, SaksopplysningType.ARBEIDSFORHOLD);
            Optional<Saksopplysning> inntektSaksopplysning = hentSaksOpplysning(alleSaksopplysninger, SaksopplysningType.INNTEKT);

            arbeidsforholdSaksopplysning.ifPresent(saksopplysning -> orgnumre.addAll(hentOrgnumreFraArbeidsforhold(saksopplysning)));
            inntektSaksopplysning.ifPresent(saksopplysning -> orgnumre.addAll(hentOrgnumreFraInntekt(saksopplysning)));

            prosessinstans.getBehandling().getSaksopplysninger().addAll(hentOrganisasjoner(orgnumre));

            Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
            fagsakService.lagre(fagsak);

        } catch (SikkerhetsbegrensningException | IkkeFunnetException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            // FIXME: MELOSYS-1316
            return;
        }
        prosessinstans.setSteg(ProsessSteg.HENT_MEDL_OPPL);
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

    private List<Saksopplysning> hentOrganisasjoner(Set<String> orgnumre) throws SikkerhetsbegrensningException, IkkeFunnetException {
        List<Saksopplysning> saksopplysninger = new ArrayList<>();
        for (String orgnr : orgnumre) {
            saksopplysninger.add(eregFasade.hentOrganisasjon(orgnr));
        }
        return saksopplysninger;
    }
}
