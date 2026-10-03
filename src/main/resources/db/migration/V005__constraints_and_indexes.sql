CREATE UNIQUE INDEX IF NOT EXISTS employee_account_username_key
    ON public.employee_account (lower(username));

CREATE UNIQUE INDEX IF NOT EXISTS employee_account_employee_id_key
    ON public.employee_account (employee_id);

CREATE INDEX IF NOT EXISTS employee_hours_employee_date_idx
    ON public.employee_hours (employee_id, date);
CREATE INDEX IF NOT EXISTS leave_details_employee_idx
    ON public.leave_details (employee_id);
CREATE INDEX IF NOT EXISTS leave_details_status_idx
    ON public.leave_details (status);
