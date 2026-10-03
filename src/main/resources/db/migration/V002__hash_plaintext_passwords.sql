CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE public.employee_account
   SET password = crypt(password, gen_salt('bf', 12))
 WHERE password IS NOT NULL
   AND password <> ''
   AND password !~ '^\$2[aby]\$\d\d\$';
