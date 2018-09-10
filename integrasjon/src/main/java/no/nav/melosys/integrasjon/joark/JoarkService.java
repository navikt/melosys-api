package no.nav.melosys.integrasjon.joark;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.DokumentTittel;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.domain.joark.JournalfoeringMangel;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.Fagsystem;
import no.nav.melosys.integrasjon.Konstanter;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.joark.behandleinngaaendejournal.BehandleInngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.ArkivSak;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Avsender;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentkategori;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.FerdigstillJournalfoeringRequest;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.meldinger.OppdaterJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.*;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.UtledJournalfoeringsbehovRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.UtledJournalfoeringsbehovResponse;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JoarkService implements JoarkFasade {

    private BehandleInngaaendeJournalConsumer behandleInngåendeJournalConsumer;
    private InngaaendeJournalConsumer inngåendeJournalConsumer;
    private JournalConsumer journalConsumer;

    @Autowired
    public JoarkService(BehandleInngaaendeJournalConsumer behandleInngåendeJournal, InngaaendeJournalConsumer inngåendeJournal, JournalConsumer journal) {
        this.behandleInngåendeJournalConsumer = behandleInngåendeJournal;
        this.inngåendeJournalConsumer = inngåendeJournal;
        this.journalConsumer = journal;
    }

    @Override
    public void ferdigstillJournalføring(String journalpostId) throws SikkerhetsbegrensningException {
        FerdigstillJournalfoeringRequest request = new FerdigstillJournalfoeringRequest();
        request.setJournalpostId(journalpostId);
        request.setEnhetId(String.valueOf(Konstanter.MELOSYS_ENHET_ID));
        try {
            behandleInngåendeJournalConsumer.ferdigstillJournalfoering(request);
        } catch (FerdigstillJournalfoeringFerdigstillingIkkeMulig | FerdigstillJournalfoeringJournalpostIkkeInngaaende
            | FerdigstillJournalfoeringUgyldigInput | FerdigstillJournalfoeringObjektIkkeFunnet e) {
            throw new IntegrasjonException(e);
        } catch (FerdigstillJournalfoeringSikkerhetsbegrensning ferdigstillJournalfoeringSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(ferdigstillJournalfoeringSikkerhetsbegrensning);
        }
    }

    @Override
    public byte[] hentDokument(String journalPostID, String dokumentID) {
        HentDokumentRequest request = new HentDokumentRequest();
        request.setDokumentId(dokumentID);
        request.setJournalpostId(journalPostID);

        Variantformater variantformat = new Variantformater();
        variantformat.setValue(Variantformat.ARKIV.toString());
        request.setVariantformat(variantformat);

        HentDokumentResponse hentDokumentResponse;
        try {
            hentDokumentResponse = journalConsumer.hentDokument(request);
        } catch (HentDokumentDokumentIkkeFunnet | HentDokumentJournalpostIkkeFunnet | HentDokumentSikkerhetsbegrensning e) {
            throw new IntegrasjonException(e);
        }
        return hentDokumentResponse.getDokument();
    }

    @Override
    public Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException {
        HentJournalpostRequest request = new HentJournalpostRequest();
        request.setJournalpostId(journalpostID);

        HentJournalpostResponse hentJournalpostResponse;
        try {
            hentJournalpostResponse = inngåendeJournalConsumer.hentJournalpost(request);
        } catch (HentJournalpostJournalpostIkkeFunnet | HentJournalpostJournalpostIkkeInngaaende | HentJournalpostUgyldigInput e) {
            throw new IntegrasjonException(e);
        } catch (HentJournalpostSikkerhetsbegrensning hentJournalpostSikkerhetsbegrensning) {
            throw new SikkerhetsbegrensningException(hentJournalpostSikkerhetsbegrensning);
        }

        InngaaendeJournalpost inngaaendeJournalpost = hentJournalpostResponse.getInngaaendeJournalpost();
        Journalpost journalpost = new Journalpost(journalpostID);
        List<Aktoer> brukerListe = inngaaendeJournalpost.getBrukerListe();
        if (!brukerListe.isEmpty()) {
            // FIXME Vi antar foreløpig at det er bare en bruker. Vi trenger en avklaring.
            if (brukerListe.size() > 1) {
                throw new IntegrasjonException("Det finnes flere brukere i journalpost " + journalpostID); 
            } else {
                Aktoer aktoer = brukerListe.get(0);
                if (aktoer instanceof Person) {
                    Person p = (Person) aktoer;
                    journalpost.setBrukerId(p.getIdent());
                } else if (aktoer instanceof Organisasjon) {
                    throw new IntegrasjonException("Organisasjon i brukerlisten er ikke støttet");
                } else {
                    throw new IntegrasjonException(aktoer.getClass().getSimpleName() + " i brukerlisten er ikke støttet");
                }
            }
        }
        journalpost.setAvsenderId(inngaaendeJournalpost.getAvsenderId());
        if (inngaaendeJournalpost.getForsendelseMottatt() != null) {
            journalpost.setForsendelseMottatt(KonverteringsUtils.xmlGregorianCalendarToLocalDateTime(inngaaendeJournalpost.getForsendelseMottatt()));
        }
        Dokumentinformasjon hoveddokument = inngaaendeJournalpost.getHoveddokument();
        journalpost.setHoveddokumentId(hoveddokument.getDokumentId());
        // FIXME mangler opplysninger (tittel og vedleggstitler) fra https://jira.adeo.no/browse/PK-51497
        journalpost.setHoveddokumentTittel(DokumentTittel.SØKNAD_MEDLEMSSKAP.getBeskrivelse());

        return journalpost;
    }

    @Override
    public void oppdaterJounalpost(String journalpostId, String dokumentID, Long gsakSaksnummer, String brukerID, String avsenderID, String avsenderNavn, String tittel, boolean medDokumentkategori)
        throws SikkerhetsbegrensningException {
        OppdaterJournalpostRequest request = new OppdaterJournalpostRequest();
        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.InngaaendeJournalpost journalpost =
                new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.InngaaendeJournalpost();
        journalpost.setJournalpostId(journalpostId);

        ArkivSak arkivSak = new ArkivSak();
        arkivSak.setArkivSakId(Long.toString(gsakSaksnummer));
        arkivSak.setArkivSakSystem(Fagsystem.GSAK.getKode());
        journalpost.setArkivSak(arkivSak);

        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Person bruker = new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Person();
        bruker.setIdent(brukerID);
        journalpost.setBruker(bruker);

        Avsender avsender = new Avsender();
        avsender.setAvsenderId(avsenderID);
        avsender.setAvsenderNavn(avsenderNavn);
        journalpost.setAvsender(avsender);

        no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentinformasjon dokumentinfo =
            new no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.informasjon.Dokumentinformasjon();
        if (medDokumentkategori) {
            Dokumentkategori dokumentkategori = new Dokumentkategori();
            dokumentkategori.setValue(DokumentKategoriKode.IS.getKode());
            dokumentinfo.setDokumentkategori(dokumentkategori);
        }
        dokumentinfo.setDokumentId(dokumentID);
        dokumentinfo.setTittel(tittel);
        journalpost.setHoveddokument(dokumentinfo);
        journalpost.setInnhold(tittel); // Innhold bruker titlen siden det ikke finnes andre grunnlag for det.

        request.setInngaaendeJournalpost(journalpost);
        try {
            behandleInngåendeJournalConsumer.oppdaterJournalpost(request);
        } catch (OppdaterJournalpostSikkerhetsbegrensning e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (OppdaterJournalpostOppdateringIkkeMulig | OppdaterJournalpostUgyldigInput | OppdaterJournalpostJournalpostIkkeInngaaende
            | OppdaterJournalpostObjektIkkeFunnet e) {
            throw new IntegrasjonException(e);
        }
    }

    @Override
    public List<JournalfoeringMangel> utledJournalfoeringsbehov(String journalpostID) throws SikkerhetsbegrensningException {
        UtledJournalfoeringsbehovRequest request = new UtledJournalfoeringsbehovRequest();
        request.setJournalpostId(journalpostID);

        try {
            UtledJournalfoeringsbehovResponse utledJournalfoeringsbehovResponse = inngåendeJournalConsumer.utledJournalfoeringsbehov(request);
            JournalpostMangler journalfoeringsbehov = utledJournalfoeringsbehovResponse.getJournalfoeringsbehov();
            return konverterTilJournalfoeringmangler(journalfoeringsbehov);
        } catch (UtledJournalfoeringsbehovSikkerhetsbegrensning s) {
            throw new SikkerhetsbegrensningException(s);
        } catch (UtledJournalfoeringsbehovUgyldigInput | UtledJournalfoeringsbehovJournalpostKanIkkeBehandles | UtledJournalfoeringsbehovJournalpostIkkeFunnet
            | UtledJournalfoeringsbehovJournalpostIkkeInngaaende  e) {
            throw new IntegrasjonException(e);
        }

    }

    List<JournalfoeringMangel> konverterTilJournalfoeringmangler(JournalpostMangler input) {
        List<JournalfoeringMangel> mangler = new ArrayList<>();

        if (input.getArkivSak() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.ARKIVSAK);
        }
        if (input.getAvsenderId() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.AVSENDERID);
        }
        if (input.getAvsenderNavn() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.AVSENDERNAVN);
        }
        if (input.getBruker() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.BRUKER);
        }
        if (input.getForsendelseInnsendt() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.FORSENDELSEINNSENDT);
        }
        if (input.getHoveddokument().getDokumentkategori() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI);
        }
        if (input.getHoveddokument().getTittel() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.HOVEDDOKUMENT_TITTEL);
        }
        if (input.getInnhold() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.INNHOLD);
        }
        if (input.getVedleggListe() != null && !input.getVedleggListe().isEmpty()) {
            mangler.add(JournalfoeringMangel.VEDLEGG);
        }
        if (input.getTema() == Journalfoeringsbehov.MANGLER) {
            mangler.add(JournalfoeringMangel.TEMA);
        }

        return mangler;
    }
}
