INSERT INTO public.employee_leave (employee_id, taken, available, total)
SELECT e.employee_id, 0, 25, 25
  FROM public.employee e
 WHERE NOT EXISTS (SELECT 1 FROM public.employee_leave l WHERE l.employee_id = e.employee_id);
