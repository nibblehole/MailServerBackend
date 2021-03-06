package Filters;

import Services.Mail;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class DateFilter extends AbstractFilter {
    /**
     *
     * @param m, mail to be judged
     * @param val, will be cast to Date
     * @return  If m.getDate().getDay() == dat.getDay()
     */
    @Override
    public boolean passesCriteria(Mail m, Object val) {

        LocalDate date = (m.getDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate());

        System.out.println(date.toString());
        System.out.println(val.toString());

        return date.toString().equals(val.toString());
    }

}
