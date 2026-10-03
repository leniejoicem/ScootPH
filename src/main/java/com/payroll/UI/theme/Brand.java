package com.payroll.UI.theme;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

public final class Brand {

    private static final String MARK = "brand/scootph-mark.svg";
    private static final String SCOOTER = "brand/scooter-illustration.svg";

    private Brand() {
    }

    public static FlatSVGIcon mark(int size) {
        return new FlatSVGIcon(MARK, size, size);
    }

    public static FlatSVGIcon scooter(int width, java.awt.Color color) {
        FlatSVGIcon icon = new FlatSVGIcon(SCOOTER, width, width * 3 / 4);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icon;
    }

    public static javax.swing.JLabel wordmark(int textSize) {
        javax.swing.JLabel label = new javax.swing.JLabel("<html><span style='color:#111827'>Scoot</span>"
                + "<span style='color:#F05A28'>PH</span></html>");
        label.setFont(Theme.font(java.awt.Font.BOLD, textSize));
        label.setIcon(scooter(textSize * 2, Theme.PRIMARY));
        label.setIconTextGap(8);
        label.getAccessibleContext().setAccessibleName("ScootPH");
        return label;
    }

    public static List<Image> windowIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 20, 24, 32, 40, 48, 64, 128, 256}) {
            icons.add(mark(size).getImage());
        }
        return icons;
    }
}
