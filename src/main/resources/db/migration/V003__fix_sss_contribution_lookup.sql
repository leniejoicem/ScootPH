CREATE OR REPLACE FUNCTION public.get_sss_contribution(emp_salary numeric) RETURNS numeric
    LANGUAGE sql STABLE
    AS $$
    SELECT contribution
      FROM public.sss
     WHERE COALESCE(cr_above, 0) <= emp_salary
     ORDER BY cr_above DESC NULLS LAST
     LIMIT 1;
$$;
