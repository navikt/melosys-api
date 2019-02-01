package no.nav.melosys.saksflyt.agent.iv.validering;

import java.util.Set;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;

public enum SendBrevValidator {
    INSTANCE;

    public static Lovvalgsperiode validerLovvalgsperiode(Set<Lovvalgsperiode> lovvalgsperioder) throws FunksjonellException {
        if (lovvalgsperioder.isEmpty()) {
            throw new FunksjonellException("Lovvalgsperiode mangler");
        }

        if (lovvalgsperioder.size() > 1) {
            throw new UnsupportedOperationException("Flere enn en"
                + " lovvalgsperiode er ikke støttet i første leveranse");
        }

        return lovvalgsperioder.iterator().next();
    }

    /**
     * Finn ut om avslagsesbrev skal sendes.
     *
     * Avslagsesbrev skal sendes dersom behandlingen har resultert i:
     * <ul>
     * <li>Fastsatt lovvalgsland er avklart og avslått</li>
     * <li>Lovvalgslandet ikke er Norge</li>
     * <li>Lovvalgbestemmelsen er 12.1, 12.2 eller 16.1</li>
     */
    public static boolean avslagsbrevSkalSendes(Behandlingsresultattyper behandlingsresultatType, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultatType == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            && lovvalgsperiode.getInnvilgelsesresultat().equals(InnvilgelsesResultat.AVSLAATT)
            && lovvalgsperiode.getLovvalgsland() != Landkoder.NO
            && erGyldigBestemmelse(lovvalgsperiode.getBestemmelse());
    }

    /**
     * Finn ut om innvilgelsesbrev skal sendes.
     * <p>
     * Innvilgelsesbrev skal sendes dersom behandlingen har resultert i:
     * <ul>
     * <li>Lovvalgsland er avklart</li>
     * <li>Innvilget lovvalgsland er Norge</li>
     * <li>Lovvalgbestemmelsen er 12.1, 12.2 eller 16.1</li>
     */
    public static boolean innvilgelsesbrevSkalSendes(Behandlingsresultattyper behandlingsresultatType, Lovvalgsperiode lovvalgsperiode) {
        return behandlingsresultatType == Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            && lovvalgsperiode.getInnvilgelsesresultat().equals(InnvilgelsesResultat.INNVILGET)
            && lovvalgsperiode.getLovvalgsland() == Landkoder.NO
            && erGyldigBestemmelse(lovvalgsperiode.getBestemmelse());
    }

    public static boolean erGyldigBestemmelse(LovvalgBestemmelse bestemmelse) {
        return bestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1
            || bestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2
            || bestemmelse == LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1;
    }
}
