package it.dmi.mail.pec.model;

import lombok.Data;

/**
 * 
 * @author biagio.tozzi
 *
 */
@Data
public class RicevutaPEC {
	
	private DatiCertificazione datiCertificazione;
	private PEC messaggioOriginale;
	
}
