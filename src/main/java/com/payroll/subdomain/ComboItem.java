package com.payroll.subdomain;

public class ComboItem {
    private Integer key;
    private String value;

    public ComboItem(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public String getValue() {
        return value;
    }

    public Integer getKey(){
        return key;
    }

    public boolean equals(Object other) {
       if (other == null) {
            return false;
        }

       if (!(other instanceof ComboItem)) {
           return false;
        }

       ComboItem item = (ComboItem) other;
       return this.getValue().equals(item.getValue());
    }
}
