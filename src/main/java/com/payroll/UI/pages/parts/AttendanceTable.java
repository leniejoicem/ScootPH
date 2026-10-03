package com.payroll.UI.pages.parts;

import com.payroll.UI.kit.DataTable;
import com.payroll.UI.kit.DataTable.Column;
import com.payroll.UI.kit.DataTable.Kind;
import com.payroll.service.AttendanceService.Day;

public final class AttendanceTable {

    private AttendanceTable() {
    }

    public static DataTable<Day> create() {
        DataTable<Day> table = new DataTable<>(null,
                Column.of("Date", Day::date, Kind.DAY, 120),
                Column.of("Time in", Day::timeIn, Kind.TIME, 90),
                Column.of("Time out", Day::timeOut, Kind.TIME, 90),
                Column.text("Hours", Day::hoursLabel, 70),
                Column.of("Status", Day::status, Kind.BADGE, 120));
        table.setEmptyMessage("No attendance", "Nothing was recorded for this month.");
        table.visibleRows(10);
        return table;
    }
}
