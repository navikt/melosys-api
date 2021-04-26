package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.exception.TekniskException;

public record Journalpost(
    String journalpostId,
    String tittel,
    Journalstatus journalstatus,
    String tema,
    Journalposttype journalposttype,
    Sak sak,
    Bruker bruker,
    AvsenderMottaker avsenderMottaker,
    String kanal,
    Collection<RelevantDato> relevanteDatoer,
    Collection<DokumentInfo> dokumenter
) {
    public no.nav.melosys.domain.arkiv.Journalpost tilDomene() {
        var journalpost = new no.nav.melosys.domain.arkiv.Journalpost(journalpostId);

        if (sak != null) {
            journalpost.setArkivSakId(sak.arkivsaksnummer());
        }

        if (bruker != null) {
            if (Brukertype.erPerson(bruker.type())) {
                if (bruker.type() == Brukertype.AKTOERID) {
                    // todo hent fnr
                } else {
                    journalpost.setBrukerId(bruker.id());
                }
            } else {
                throw new UnsupportedOperationException("Støtter ikke bruker med type " + bruker.type());
            }
        }

        if (avsenderMottaker != null) {
            journalpost.setAvsenderId(avsenderMottaker.id());
            journalpost.setAvsenderNavn(avsenderMottaker.navn());
            journalpost.setAvsenderType(AvsenderMottakerType.tilDomene(avsenderMottaker.type()));
            journalpost.setKorrespondansepartId(avsenderMottaker.id());
            journalpost.setKorrespondansepartNavn(avsenderMottaker.navn());
        }

        journalpost.setErFerdigstilt(erFerdigstilt());
        journalpost.setForsendelseJournalfoert(hentForsendelseJournalført());
        journalpost.setForsendelseMottatt(hentForsendelseMottatt());
        journalpost.setHoveddokument(hentHoveddokument());
        journalpost.setInnhold(tittel);
        journalpost.setJournalposttype(Journalposttype.tilDomene(journalposttype));
        journalpost.getVedleggListe().addAll(hentVedlegg());
        journalpost.setMottaksKanal(kanal);
        journalpost.setTema(tema);

        return journalpost;
    }

    private ArkivDokument hentHoveddokument() {
        return Optional.ofNullable(dokumenter)
            .stream().flatMap(Collection::stream)
            .map(DokumentInfo::tilArkivDokument).findFirst()
            .orElseThrow(() -> new TekniskException("Journalpost " + journalpostId + " har ingen hoveddokument"));
    }

    private Collection<ArkivDokument> hentVedlegg() {
        return dokumenter.stream().skip(1L) // hopper over hoveddokumentet (som alltid ligger først).
            .map(DokumentInfo::tilArkivDokument)
            .collect(Collectors.toList());
    }

    private Instant hentForsendelseMottatt() {
        if (erInngående()) {
            return relevanteDatoer.stream()
                .filter(RelevantDato::harDatotypeRegistrert)
                .map(RelevantDato::dato)
                .map(Journalpost::tilInstant)
                .findFirst()
                .orElse(null);
        }

        return null;
    }

    private Instant hentForsendelseJournalført() {
        return relevanteDatoer.stream()
            .filter(RelevantDato::harDatotypeJournalført)
            .map(RelevantDato::dato)
            .map(Journalpost::tilInstant)
            .findFirst()
            .orElse(null);
    }

    private static Instant tilInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private boolean erFerdigstilt() {
        if (erInngående()) {
            return journalstatus == Journalstatus.JOURNALFOERT;
        }
        if (erUtgående()) {
            return journalstatus == Journalstatus.FERDIGSTILT;
        }
        return false;
    }

    private boolean erInngående() {
        return Journalposttype.I == journalposttype;
    }

    private boolean erUtgående() {
        return Journalposttype.U == journalposttype;
    }

    private boolean erNotat() {
        return Journalposttype.N == journalposttype;
    }
}
