package it.dmi.mail.pec;

import it.dmi.mail.pec.exception.PECParserException;
import it.dmi.mail.pec.model.Messaggio;
import it.dmi.mail.pec.parser.PECMessageParser;
import it.dmi.mail.pec.util.MimeMessageUtils;
import org.junit.jupiter.api.Test;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 
 * @author biagio.tozzi
 *
 */
public class PECParserTest {



    @Test
	public void testReadPec1() throws PECParserException, IOException {

		try (InputStream emlInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("[XDCR] xdcr_2022_02_00583.txt.eml")) {
			MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(emlInputStream, null);
			Messaggio messaggio = PECMessageParser.getInstance().parse(mimeMessage);
			System.out.println(messaggio);
			String expectedSubject = "[XDCR] xdcr_2022_02_00583.txt";
			String actualSubject = messaggio.getPec().getOggetto();
			assertEquals(expectedSubject, actualSubject);
		}

	}
    
	
//	@Test
//	public void test1() throws PECParserException, IOException {
//				
//		try (InputStream emlInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("message.eml")) {
//			MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(emlInputStream, null);
//			Messaggio messaggio = PECMessageParser.getInstance().parse(mimeMessage);
//			System.out.println(messaggio);
//			// Test...
//		}
//		
//	}
//	
//	@Test
//	public void test2() throws PECParserException, IOException {
//				
//		try (InputStream emlInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("message2.eml")) {
//			MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(emlInputStream, null);
//			Messaggio messaggio = PECMessageParser.getInstance().parse(mimeMessage);
//			System.out.println(messaggio);
//			// Test...
//		}
//		
//	}
//	
//	@Test
//	public void test3() throws PECParserException, IOException {
//				
//		try (InputStream emlInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("message3.eml")) {
//			MimeMessage mimeMessage = MimeMessageUtils.createMimeMessage(emlInputStream, null);
//			Messaggio messaggio = PECMessageParser.getInstance().parse(mimeMessage);
//			System.out.println(messaggio);
//			// Test...
//		}
//		
//	}

}
