package com.payroll.UI.pages.parts;

import com.payroll.domain.Person;
import com.payroll.subdomain.ComboItem;
import java.util.Comparator;
import java.util.List;
import javax.swing.JComboBox;

public class EmployeePicker extends JComboBox<ComboItem> {

    private boolean populating;

    public EmployeePicker() {
        setMaximumRowCount(14);
    }

    @Override
    protected void fireActionEvent() {
        if (!populating) {
            super.fireActionEvent();
        }
    }

    public void setPeople(List<Person> people) {
        Integer selected = employeeId();
        populating = true;
        try {
            fill(people);
        } finally {
            populating = false;
        }
        if (selected != null) {
            selectEmployee(selected);
        }
    }

    private void fill(List<Person> people) {
        removeAllItems();
        people.stream()
                .sorted(Comparator.comparing((Person p) -> p.getLastName().toLowerCase())
                        .thenComparing(p -> p.getFirstName().toLowerCase()))
                .forEach(p -> addItem(new ComboItem(p.getEmpID(),
                        p.getLastName() + ", " + p.getFirstName() + "  ·  #" + p.getEmpID())));
    }

    public Integer employeeId() {
        ComboItem item = (ComboItem) getSelectedItem();
        return item == null ? null : item.getKey();
    }

    public void selectEmployee(int id) {
        for (int i = 0; i < getItemCount(); i++) {
            if (Integer.valueOf(id).equals(getItemAt(i).getKey())) {
                setSelectedIndex(i);
                return;
            }
        }
    }
}
