package it.dmi.mail.pec.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import it.dmi.mail.pec.exception.PECParserException;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author biagio.tozzi
 *
 */
@Slf4j
public class MimeMessageUtils {
	
	private static final String FORMATO_DATA = "dd-MM-yyyy HH:mm";
//	private static final ZoneId ZI = ZoneId.of("Europe/Rome");
	private static final TimeZone ZI = TimeZone.getTimeZone("Europe/Rome");


	private static final String BODYSTRUCTURE_LOADING_ERROR = "Unable to load BODYSTRUCTURE";

	public static boolean isRicevuta(MimeMessage mimeMessage) throws PECParserException {
		try {
			return mimeMessage.getHeader(PECConstants.X_RICEVUTA) != null;
			
		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione dell'header", e);
			throw new PECParserException("Errore durante l'elaborazione dell'header", e);
		}
	}
	
	public static boolean isEmailOrdinaria(MimeMessage mimeMessage) throws PECParserException {
		
		try {
			return mimeMessage.getHeader(PECConstants.X_TRASPORTO, ",") == null && mimeMessage.getHeader(PECConstants.X_RICEVUTA, ",") == null;
			
		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione dell'header", e);
			throw new PECParserException("Errore durante l'elaborazione dell'header", e);
		}
	}
	
	public static boolean isPEC(MimeMessage mimeMessage) throws PECParserException {
		try {
			return mimeMessage.getHeader(PECConstants.X_TRASPORTO, ",") != null;
			
		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione dell'header", e);
			throw new PECParserException("Errore durante l'elaborazione dell'header", e);
		}
	}
	
	public static MimeMessage createMimeMessage(InputStream inputStream, Properties properties)
			throws PECParserException {

		try {
			return new MimeMessage(Session.getDefaultInstance(properties != null ? properties : System.getProperties()),
					inputStream);
			
		} catch (MessagingException e) {
			log.error("Errore durante la creazione del MimeMessage", e);
			throw new PECParserException("Errore durante la creazione del MimeMessage", e);
		}
	}

	public static boolean isMimeType(Part part, String mimeType) throws PECParserException {

		try {
			return part.isMimeType(mimeType);

		} catch (MessagingException e) {
			log.error("Errore durante la verifica del mime type {} per: {}", mimeType, getDescription(part));
			throw new PECParserException(
					"Errore durante la verifica del mime type " + mimeType + " per: " + getDescription(part), e);
		}
	}

	public static String getDescription(Part part) {

		try {
			return part.getDescription();

		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione della descrizione", e);
			return "[NON_DISPONIBILE]";
		}
	}

	public static String getDisposition(Part part) throws PECParserException {

		try {
			return part.getDisposition();

		} catch (MessagingException e) {
			log.error("Errore durante la lettura della disposition per: {}", getDescription(part), e);
			throw new PECParserException("Errore durante la lettura della disposition per: " + getDescription(part), e);
		}
	}

	public static String decodeText(String text) throws PECParserException {

		try {
			return MimeUtility.decodeText(text);

		} catch (UnsupportedEncodingException e) {
			log.error("Errore durante la decodifica del testo: {}", text, e);
			throw new PECParserException("Errore durante la decodifica del testo: " + text, e);
		}
	}

	public static int getCount(Multipart multiPart) throws PECParserException {

		try {
			return multiPart.getCount();

		} catch (MessagingException e) {
			log.error("Errore durante la lettura del parametro count", e);
			throw new PECParserException("Errore durante la lettura del parametro count", e);
		}
	}

	public static String getFileName(Part part) throws PECParserException {
		try {
			return part.getFileName();

		} catch (MessagingException e) {
			log.error("Errore durante la lettura del nome del file di: {}", getDescription(part), e);
			throw new PECParserException("Errore durante la lettura del nome del file di: " + getDescription(part), e);
		}
	}

	public static BodyPart getBodyPart(Multipart multiPart, int index) throws PECParserException {

		try {
			return multiPart.getBodyPart(index);

		} catch (MessagingException e) {
			log.error("Errore durante la lettura della parte numero {}", index, e);
			throw new PECParserException("Errore durante la lettura della parte numero: " + index, e);
		}

	}

	public static String getXAttachmentID(MimePart part) {
		List<String> values;

		try {
			values = MimeMessageUtils.getHeaderValues(PECConstants.X_ATTACHMENT_ID, part);
		} catch (PECParserException e) {
			return null;
		}

		return values.isEmpty() ? null : values.get(0);
	}

	public static String getHeaderValue(String headerKey, Part part) throws PECParserException {

		List<String> values;

		try {
			values = MimeMessageUtils.getHeaderValues(headerKey, part);
		} catch (PECParserException e) {
			return null;
		}

		return values.isEmpty() ? null : values.get(0);

	}

	public static List<String> getHeaderValues(String headerKey, Part part) throws PECParserException {

		try {
			String[] res = part.getHeader(headerKey);
			if (res != null) {
				List<String> headerValues = new ArrayList<String>();
				for (String value : res) {
					headerValues.add(value);
				}
				return headerValues;
			}


			return new ArrayList<>();

		} catch (MessagingException e) {
			log.error("Errore durante l'estrazione dell'header: {} per di {}", headerKey, getDescription(part), e);
			throw new PECParserException("Errore durante l'estrazione del'header " + headerKey + " data handler per: "
					+ getDescription(part), e);
		}

	}

	public static DataHandler getDataHandler(Part part) throws PECParserException {

		try {
			return part.getDataHandler();

		} catch (MessagingException e) {
			log.error("Errore durante l'estrazione del data handler per: {}", getDescription(part), e);
			throw new PECParserException("Errore durante l'estrazione del data handler per: " + getDescription(part),
					e);
		}

	}

	public static String getContentType(Part part) throws PECParserException {

		try {
			return part.getContentType();

		} catch (MessagingException e) {
			log.error("Errore durante l'estrazione del content type: {}", getDescription(part), e);
			throw new PECParserException("Errore durante l'estrazione del content type: " + getDescription(part), e);
		}

	}

	public static Object getContent(Part part) throws PECParserException {
		Object content;

		try {
			content = part.getContent();

		} catch (IOException | MessagingException e) {

			if (part instanceof MimeMessage && BODYSTRUCTURE_LOADING_ERROR.equalsIgnoreCase(e.getMessage())) {

				try {
					content = new MimeMessage((MimeMessage) part).getContent();

				} catch (IOException | MessagingException e1) {
					log.error("Errore durante la lettura del contenuto di: {}", getDescription(part), e);
					throw new PECParserException("Errore durante la lettura del contenuto di: " + getDescription(part),
							e);
				}

			} else if (isMimeType(part, MimeTypesUtil.CONTENT_TYPE_TEXT)) {

				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					IOUtils.fastCopy(part.getInputStream(), os);
					content = new String(os.toByteArray(), StandardCharsets.US_ASCII);

				} catch (IOException | MessagingException e1) {
					throw new PECParserException("Errore durante la lettura del contenuto di: " + getDescription(part),
							e);
				}

			} else {
				log.error("Errore durante la lettura del contenuto di: {}", getDescription(part), e);
				throw new PECParserException("Errore durante la lettura del contenuto di: " + getDescription(part), e);
			}
		}

		return content;
	}

	public static String getUniqueMessageID(MimeMessage mimeMessage) throws PECParserException {
		
		DateFormat df = new SimpleDateFormat(FORMATO_DATA);
		StringBuilder res = new StringBuilder();
		
		//
		try {
			if (mimeMessage.getSender() != null && mimeMessage.getSender() instanceof InternetAddress) {
				InternetAddress ia = (InternetAddress) mimeMessage.getSender();
				res.append(ia.getAddress()).append("_");
			}
			
		} catch (MessagingException e) {
		}
		
		//
		try {
			if (mimeMessage.getAllRecipients() != null) {
				List<String> destinatari = new ArrayList<String>();
				Address[] recipients = mimeMessage.getAllRecipients();
				for (Address r : recipients) {
					if (r != null && r instanceof InternetAddress) {
						InternetAddress internetAddress = (InternetAddress) r;
						destinatari.add(internetAddress.getAddress());
					}
				}
				if (!destinatari.isEmpty()) {
					for (String destinatario : destinatari) {
						if (res.length() > 0) {
							res.append("_");
						}
						res.append(destinatario);
					}
				}
			}

		} catch (MessagingException e) {
		}
		
		boolean sd = false;
		boolean rd = false;
		
		//
		try {
			if (mimeMessage.getSentDate() != null) {
				res.append(df.format(mimeMessage.getSentDate())).append("_");
				sd = true;
			}
			
		} catch (MessagingException e) {
		}
		
		//
		try {
			if (mimeMessage.getReceivedDate() != null) {
				res.append(df.format(mimeMessage.getReceivedDate())).append("_");
				rd = true;
			}
			
		} catch (MessagingException e) {
		}
		
		if (res.length() == 0 || (!rd && !sd)) {
			return new Timestamp(Calendar.getInstance(ZI).getTimeInMillis()).hashCode() + UUID.randomUUID().toString();

		}
		
		try {
			return new String(MessageDigest.getInstance("SHA-256").digest(res.toString().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
			
		} catch (NoSuchAlgorithmException e) {
			throw new PECParserException("Errore in fase di elaborazione del messageID", e);
		}
	}
}
