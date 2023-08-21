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
public enum TipoPostaCert {
	ACCETTAZIONE("accettazione"),
	NON_ACCETTAZIONE("non-accettazione"),
	PRESA_IN_CARICO("presa-in-carico"),
	AVVENUTA_CONSEGNA("avvenuta-consegna"),
	POSTA_CERTIFICATA("posta-certificata"),
	ERRORE("errore"),
	ERRORE_CONSEGNA("errore-consegna"),
	PREAVVISO_ERRORE_CONSEGNA("preavviso-errore-consegna"),
	RILEVAZIONE_VIRUS("rilevazione-virus");
	
	private final String descrizione;
	
	public static TipoPostaCert from(String descrizione) {
		TipoPostaCert result = null;
		for (TipoPostaCert t : TipoPostaCert.values()) {
			if (t.getDescrizione().equals(descrizione)) {
				result = t;
				break;
			}
		}
		return result;

	}
}
