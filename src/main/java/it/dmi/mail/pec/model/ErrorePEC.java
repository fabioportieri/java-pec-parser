package it.dmi.mail.pec.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 
 * @author biagio.tozzi
 *
 */
@AllArgsConstructor
@Getter
public enum ErrorePEC {
	NESSUNO("nessuno"), //
	NO_DEST("no-dest"), //
	NO_DOMINIO("no-dominio"), //
	VIRUS("virus"), //
	ALTRO("altro"); //
	
	private final String descrizione;
	
	public static ErrorePEC from(String descrizione) {
		ErrorePEC result = null;
		for (ErrorePEC t : ErrorePEC.values()) {
			if (t.getDescrizione().equals(descrizione)) {
				result = t;
				break;
			}
		}
		return result;

	}
}
