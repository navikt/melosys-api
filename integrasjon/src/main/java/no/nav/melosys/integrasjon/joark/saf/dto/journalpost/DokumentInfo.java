package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import java.util.List;

import no.nav.melosys.domain.arkiv.ArkivDokument;

public record DokumentInfo(
    String dokumentInfoId,
    String tittel,
    String brevkode,
    List<LogiskVedlegg> logiskeVedlegg,
    List<DokumentVariant> dokumentvarianter
) {
    public ArkivDokument tilArkivDokument() {
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(dokumentInfoId);
        arkivDokument.setTittel(tittel);
        arkivDokument.setNavSkjemaID(brevkode);

        if (logiskeVedlegg != null) {
            logiskeVedlegg.stream().map(LogiskVedlegg::tilDomene).forEach(arkivDokument.getLogiskeVedlegg()::add);
        }

        if (dokumentvarianter != null) {
            dokumentvarianter.stream().map(DokumentVariant::tilDomene).forEach(arkivDokument.getDokumentVarianter()::add);
        }

        return arkivDokument;
    }
}
