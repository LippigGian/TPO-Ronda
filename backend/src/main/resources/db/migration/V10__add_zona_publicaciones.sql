-- Zona aproximada del vendedor (barrio/localidad). Se muestra en el listado sin revelar la dirección exacta.
ALTER TABLE ronda.publicaciones ADD COLUMN zona VARCHAR(80);

-- Backfill para publicaciones existentes: último tramo de la dirección ("Calle 123, Palermo" -> "Palermo").
UPDATE ronda.publicaciones
SET zona = LEFT(TRIM(REGEXP_REPLACE(direccion, '^.*,', '')), 80)
WHERE zona IS NULL AND POSITION(',' IN direccion) > 0;

CREATE INDEX idx_publicaciones_estado_created_at
    ON ronda.publicaciones (estado_publicacion, created_at DESC);
