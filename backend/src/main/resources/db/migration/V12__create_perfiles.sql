-- Perfil editable de cada usuario (punto 2 del TPO).
-- Tabla propia en lugar de agregar columnas a "usuarios": asi la feature de perfil
-- no modifica el esquema de autenticacion y cada tabla tiene un unico responsable.
CREATE TABLE ronda.perfiles (
    usuario_id BIGINT PRIMARY KEY REFERENCES ronda.usuarios(id) ON DELETE CASCADE,
    nombre VARCHAR(120),
    telefono VARCHAR(30),
    zona VARCHAR(120),
    foto_archivo VARCHAR(255),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
