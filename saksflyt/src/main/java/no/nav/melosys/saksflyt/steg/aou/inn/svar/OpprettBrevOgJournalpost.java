package no.nav.melosys.saksflyt.steg.aou.inn.svar;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.DokumentVariant;
import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettBrevOgJournalpost extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettBrevOgJournalpost.class);

    private static final String SENTRAL_UTSKRIFT = "S";
    private static final String MEDLEMSKAP_OG_AVGIFT = "4530";
    private static final String UNNTAK_FRA_MEDLEMSKAP = "UFM";
    private static final String ARKIV = "ARKIV";
    private static final String UTENLANDSK_ORGANISASJON = "UTL_ORG";

    private final EessiService eessiService;
    private final JoarkFasade joarkFasade;
    private final TpsFasade tpsFasade;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    @Autowired
    public OpprettBrevOgJournalpost(EessiService eessiService, JoarkFasade joarkFasade, TpsFasade tpsFasade, UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        this.eessiService = eessiService;
        this.joarkFasade = joarkFasade;
        this.tpsFasade = tpsFasade;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_SVAR_OPPRETT_JOURNALPOST;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = joarkFasade.opprettJournalpost(lagJournalpost(prosessinstans), true);

        log.info("Opprettet journalpost {} for behandling {}", journalpostId, prosessinstans.getBehandling().getId());
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, journalpostId);
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_SVAR_DISTRIBUER_JOURNALPOST);
    }

    private OpprettJournalpost lagJournalpost(Prosessinstans prosessinstans) throws MelosysException {
        Behandling behandling = prosessinstans.getBehandling();
        Fagsak fagsak = behandling.getFagsak();

        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(lagFysiskDokument(behandling.getId()));
        opprettJournalpost.setArkivSakId(fagsak.getGsakSaksnummer().toString());
        opprettJournalpost.setMottaksKanal(SENTRAL_UTSKRIFT);
        opprettJournalpost.setJournalposttype(Journalposttype.UT);
        opprettJournalpost.setJournalførendeEnhet(MEDLEMSKAP_OG_AVGIFT);
        opprettJournalpost.setTema(UNNTAK_FRA_MEDLEMSKAP);

        Korrespondansepart korrespondansepart = hentKorrespondansepart(fagsak);
        opprettJournalpost.setKorrespondansepartId(korrespondansepart.id);
        opprettJournalpost.setKorrespondansepartNavn(korrespondansepart.navn);
        opprettJournalpost.setKorrespondansepartIdType(korrespondansepart.idType);
        opprettJournalpost.setBrukerId(tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId()));

        opprettJournalpost.setInnhold(opprettJournalpost.getHoveddokument().getTittel());

        return opprettJournalpost;
    }

    private FysiskDokument lagFysiskDokument(Long behandlingID) throws MelosysException {
        SedType sedType = eessiService.hentSedTypeForAnmodningUnntakSvar(behandlingID);

        FysiskDokument fysiskDokument = new FysiskDokument();
        fysiskDokument.setDokumentKategori(DokumentKategoriKode.SED.getKode());
        fysiskDokument.setTittel(hentTittelForSedType(sedType));
        fysiskDokument.setBrevkode(sedType.name());
        fysiskDokument.setDokumentVarianter(Collections.singletonList(lagArkivVariant(behandlingID, sedType)));

        return fysiskDokument;
    }

    private DokumentVariant lagArkivVariant(Long behandlingID, SedType sedType) throws MelosysException {
        DokumentVariant dokumentVariant = new DokumentVariant();
        dokumentVariant.setVariantFormat(ARKIV);
        dokumentVariant.setFiltype(DokumentVariant.Filtype.PDFA);
        dokumentVariant.setData(eessiService.genererSedForhåndsvisning(behandlingID, sedType));

        return dokumentVariant;
    }

    private static String hentTittelForSedType(SedType sedType) {
        switch (sedType) {
            case A002:
                return "Delvis eller fullt avslag på søknad om unntak";
            case A011:
                return "Innvilgelse av søknad om unntak";
            default:
                throw new IllegalArgumentException("Kan ikke opprette journalpost av sed-type " + sedType);
        }
    }

    private Korrespondansepart hentKorrespondansepart(Fagsak fagsak) throws TekniskException {
        Landkoder landkode = fagsak.hentMyndighetLandkode();
        Aktoer aktør = fagsak.hentMyndigheter().stream().findFirst()
            .orElseThrow(() -> new TekniskException("Finner ingen myndighet for fagsak " + fagsak.getSaksnummer()));

        String navn = utenlandskMyndighetRepository.findByLandkode(landkode).map(u -> u.navn).orElse("");
        return new Korrespondansepart(aktør.getInstitusjonId(), navn, UTENLANDSK_ORGANISASJON);
    }

    private static class Korrespondansepart {
        private String id;
        private String navn;
        private String idType;

        Korrespondansepart(String id, String navn, String idType) {
            this.id = id;
            this.navn = navn;
            this.idType = idType;
        }
    }
}
