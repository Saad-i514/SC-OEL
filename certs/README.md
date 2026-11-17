# Supabase public CA certificate

Downloaded from the URL configured in Supabase's official dashboard source:
https://supabase-downloads.s3-ap-southeast-1.amazonaws.com/prod/ssl/prod-ca-2021.crt

Source: https://github.com/supabase/supabase/blob/master/apps/studio/hooks/custom-content/custom-content.json

This is a public CA certificate, not a private key or credential. JDBC uses it with sslmode=verify-full to verify the certificate chain and pooler hostname. Replace it from Supabase's official distribution when the CA rotates; do not disable verification.
