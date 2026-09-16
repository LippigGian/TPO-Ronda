ALTER TABLE ronda.usuarios
    ALTER COLUMN email_verificado SET DEFAULT FALSE;

CREATE TABLE ronda.otp_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_otp_codes_purpose CHECK (purpose IN ('REGISTRO', 'LOGIN', 'RECUPERO_CONTRASENA')),
    CONSTRAINT ck_otp_codes_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_otp_codes_pending
    ON ronda.otp_codes (email, purpose, created_at DESC)
    WHERE used_at IS NULL;
