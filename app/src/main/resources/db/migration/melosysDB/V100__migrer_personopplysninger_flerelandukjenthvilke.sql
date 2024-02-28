UPDATE MOTTATTEOPPLYSNINGER
SET DATA = REPLACE(
    DATA,
    '"erUkjenteEllerAlleEosLand"',
    '"flereLandUkjentHvilke"'
           )
WHERE DATA LIKE '%"erUkjenteEllerAlleEosLand"%';
