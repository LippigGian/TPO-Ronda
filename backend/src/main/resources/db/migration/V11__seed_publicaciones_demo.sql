-- Datos SOLO para desarrollo local: un vendedor de prueba con publicaciones para poder probar el Home y el Detalle
-- con una cuenta distinta a demo@ronda.com. Misma contraseña que el usuario demo (ronda123).
INSERT INTO ronda.usuarios (email, password_hash, nombre_usuario, email_verificado, activo)
VALUES ('vendedor@ronda.com',
        '$2a$10$CgkjsNochMtpfl/qOglB7u.bBGff7K8Fj4ynZ7S2vz4Yt3JMFnqp6',
        'Vendedor demo', TRUE, TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO ronda.publicaciones
    (vendedor_id, titulo, descripcion, categoria, precio, estado_articulo, direccion, zona, latitud, longitud, created_at, updated_at)
SELECT u.id, d.titulo, d.descripcion, d.categoria, d.precio, d.estado, d.direccion, d.zona, d.latitud, d.longitud,
       NOW() - d.antiguedad, NOW() - d.antiguedad
FROM ronda.usuarios u
CROSS JOIN (VALUES
    ('Bicicleta rodado 29', 'Mountain bike aluminio, 21 velocidades, frenos a disco. Muy poco uso.', 'Deportes', 185000.00, 'COMO_NUEVO', 'Av. Cabildo 2040, Belgrano', 'Belgrano', -34.561700, -58.456100, INTERVAL '1 hour'),
    ('Notebook Lenovo 14"', 'Core i5, 16 GB de RAM, SSD 512 GB. Incluye cargador original.', 'Tecnología', 620000.00, 'USADO', 'Honduras 5300, Palermo', 'Palermo', -34.587600, -58.430400, INTERVAL '5 hours'),
    ('Sillón de 3 cuerpos', 'Sillón de pana gris, sin manchas. Retiro por el domicilio.', 'Hogar', 240000.00, 'USADO', 'Av. Rivadavia 6100, Caballito', 'Caballito', -34.619000, -58.446700, INTERVAL '1 day'),
    ('Campera de cuero', 'Talle M, cuero vacuno negro. Nunca usada, con etiqueta.', 'Ropa', 130000.00, 'NUEVO', 'Av. Santa Fe 3200, Recoleta', 'Recoleta', -34.588300, -58.409000, INTERVAL '2 days'),
    ('Guitarra criolla', 'Guitarra criolla de estudio con funda. Cuerdas nuevas.', 'Instrumentos', 95000.00, 'COMO_NUEVO', 'Av. Corrientes 5000, Villa Crespo', 'Villa Crespo', -34.598700, -58.437200, INTERVAL '3 days'),
    ('Cafetera Nespresso', 'Funciona perfecto, incluye 20 cápsulas de regalo.', 'Hogar', 60000.00, 'USADO', 'Av. del Libertador 7000, Núñez', 'Núñez', -34.548800, -58.457500, INTERVAL '4 days'),
    ('iPhone 12 128 GB', 'Batería al 88%, sin rayones. Con caja y cable.', 'Tecnología', 450000.00, 'USADO', 'Av. Maipú 1500, Vicente López', 'Vicente López', -34.526000, -58.479000, INTERVAL '6 days'),
    ('Mesa de comedor', 'Mesa de madera maciza para 6 personas, 160x90 cm.', 'Hogar', 210000.00, 'COMO_NUEVO', 'Av. Directorio 1200, Flores', 'Flores', -34.629000, -58.462000, INTERVAL '8 days'),
    ('Zapatillas running talle 42', 'Usadas dos veces, en caja original.', 'Ropa', 70000.00, 'COMO_NUEVO', 'Av. Cramer 2500, Belgrano', 'Belgrano', -34.556000, -58.461000, INTERVAL '9 days'),
    ('Set de pesas 20 kg', 'Mancuernas ajustables con barra y discos.', 'Deportes', 55000.00, 'USADO', 'Av. Independencia 3800, Almagro', 'Almagro', -34.612000, -58.417000, INTERVAL '12 days')
) AS d(titulo, descripcion, categoria, precio, estado, direccion, zona, latitud, longitud, antiguedad)
WHERE u.email = 'vendedor@ronda.com';
