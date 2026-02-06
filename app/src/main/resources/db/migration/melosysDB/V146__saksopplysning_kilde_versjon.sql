-- Legger til versjon-kolonne for optimistisk låsing på SaksopplysningKilde.
-- Dette løser race condition ved DELETE operasjoner som oppstår med orphanRemoval=true.
ALTER TABLE saksopplysning_kilde ADD versjon NUMBER(19) DEFAULT 0;
