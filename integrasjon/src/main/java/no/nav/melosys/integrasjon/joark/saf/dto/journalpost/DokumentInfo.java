package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        arkivDokument.setLogiskeVedleggTitler(logiskeVedlegg
            .stream()
            .filter(Objects::nonNull)
            .map(LogiskVedlegg::tilDomene)
            .collect(Collectors.toList())
        );

        arkivDokument.setDokumentVarianter(dokumentvarianter
            .stream()
            .filter(Objects::nonNull)
            .map(DokumentVariant::tilDomene)
            .collect(Collectors.toList())
        );

        return arkivDokument;
    }
}
