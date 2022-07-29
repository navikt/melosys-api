package no.nav.melosys.service.kontroll.feature.unntaksperiode;

import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.data.UnntaksperiodeKontrollData;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.kontroll.UnntaksperiodeKontrollsett;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.validering.Kontrollfeil;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class UnntaksperiodeKontrollService {
    private final SaksopplysningerService saksopplysningerService;

    public UnntaksperiodeKontrollService(SaksopplysningerService saksopplysningerService) {
        this.saksopplysningerService = saksopplysningerService;
    }

    public void kontrollPeriode(Long behandlingID, ErPeriode periode) {
        SedDokument sedDokument = hentSedDokument(behandlingID);
        kontrollPeriode(sedDokument, periode);
    }

    public void kontrollPeriode(SedDokument sedDokument, ErPeriode periode) {
        List<Kontrollfeil> feilmeldinger = utførKontroll(sedDokument.getSedType(), periode);
        sjekkFeilmeldinger(feilmeldinger);
    }

    @NotNull
    private SedDokument hentSedDokument(Long behandlingID) {
        return saksopplysningerService.finnSedOpplysninger(behandlingID).orElseThrow(() -> {
            String feilmelding = "Ugyldig bruk av API for behandling" +
                " med behandlingID '%s'. Mangler SED Dokument.".formatted(behandlingID);
            throw new FunksjonellException(feilmelding);
        });
    }

    private List<Kontrollfeil> utførKontroll(SedType sedType, ErPeriode periode) {
        UnntaksperiodeKontrollData kontrollData = new UnntaksperiodeKontrollData(periode.getFom(), periode.getTom());
        return UnntaksperiodeKontrollsett.hentRegelsett(sedType)
            .stream()
            .map(f -> f.apply(kontrollData))
            .filter(Objects::nonNull)
            .toList();
    }

    private void sjekkFeilmeldinger(List<Kontrollfeil> feilmeldinger) throws ValideringException {
        if (!feilmeldinger.isEmpty()) {
            throw new ValideringException("Validering av unntaksperiode feilet",
                feilmeldinger.stream().map(Kontrollfeil::tilDto).toList());
        }
    }
}
