package app.shazin.aharemen;

import java.math.RoundingMode;
import java.text.DecimalFormat;

final class WidgetFormat {
    static String whole(double value){
        if(!Double.isFinite(value)||value<0)return "—";
        DecimalFormat format=new DecimalFormat("#,##0");format.setRoundingMode(RoundingMode.HALF_UP);return format.format(value);
    }
}
