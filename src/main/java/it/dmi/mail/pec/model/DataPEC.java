package it.dmi.mail.pec.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 
 * @author biagio.tozzi
 *
 */
@Data
@AllArgsConstructor
public class DataPEC {
	
	private static final String DATA_PEC_PATTERN = "dd/MM/yyyy HH:mm:ss";

	private String zona;
	private String giorno;
	private String ora;
	
	public Date getDate() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATA_PEC_PATTERN);
		dateFormat.setTimeZone(TimeZone.getTimeZone(this.zona));

		Date result = null;
		try {
			result = dateFormat.parse(this.giorno + " " + this.ora);
		} catch (ParseException e) {
			// Handle ParseException
			e.printStackTrace();
		}

		return result;

	}
}
