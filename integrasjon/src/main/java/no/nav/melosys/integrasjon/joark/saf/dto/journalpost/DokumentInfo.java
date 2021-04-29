package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import java.util.List;

import no.nav.melosys.domain.arkiv.ArkivDokument;

public record DokumentInfo(
    String dokumentInfoId,
    String tittel,
    String brevkode,
    List<LogiskVedlegg> logiskeVedlegg
) {
    public ArkivDokument tilArkivDokument() {
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(dokumentInfoId);
        arkivDokument.setTittel(tittel);
        arkivDokument.setNavSkjemaID(brevkode);

        if (logiskeVedlegg != null) {
            logiskeVedlegg.stream().map(LogiskVedlegg::tilDomene).forEach(arkivDokument.getLogiskeVedlegg()::add);
        }
        return arkivDokument;
    }
}
