-- Usuario compartido solo para facilitar las pruebas locales del equipo.
-- La contraseña se guarda con BCrypt, igual que las cuentas registradas desde la app.
INSERT INTO ronda.usuarios (
    email,
    password_hash,
    nombre_usuario,
    email_verificado,
    activo
) VALUES (
    'demo@ronda.com',
    '$2a$10$CgkjsNochMtpfl/qOglB7u.bBGff7K8Fj4ynZ7S2vz4Yt3JMFnqp6',
    'Usuario demo',
    TRUE,
    TRUE
)
ON CONFLICT (email) DO NOTHING;
