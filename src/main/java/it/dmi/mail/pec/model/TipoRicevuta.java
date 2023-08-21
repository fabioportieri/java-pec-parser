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
public enum TipoRicevuta {
	COMPLETA("completa"), //
	BREVE("breve"), //
	SINTETICA("sintetica"); //

	private final String descrizione;
	
	public static TipoRicevuta from(String descrizione) {
		TipoRicevuta result = null;
		for (TipoRicevuta t : TipoRicevuta.values()) {
			if (t.getDescrizione().equals(descrizione)) {
				result = t;
				break;
			}
		}
		return result;			
	}
}
