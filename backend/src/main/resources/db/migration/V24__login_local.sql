-- Login local con correo y contraseña (complemento de RF-17, decisión
-- DD-04): permite probar y demostrar el flujo de usuario común sin
-- depender de la allowlist del modo testing de Google OAuth.
-- google_id pasa a ser opcional: una cuenta local no lo tiene.

ALTER TABLE usuario
    ALTER COLUMN google_id DROP NOT NULL,
    ADD COLUMN password_hash VARCHAR(100);
