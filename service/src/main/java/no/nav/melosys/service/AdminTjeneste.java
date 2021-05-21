package no.nav.melosys.service;

import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface AdminTjeneste {
    String API_KEY_HEADER = "X-MELOSYS-ADMIN-APIKEY";

    String getApiKey();

    default void validerApikey(String value) {
        if (!getApiKey().equals(value)) {
            throw new SikkerhetsbegrensningException("Trenger gyldig apikey");
        }
    }
}
