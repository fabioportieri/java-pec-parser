package it.dmi.mail.pec.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import it.dmi.mail.pec.exception.PECParserException;
import it.dmi.mail.pec.model.Allegato;
import it.dmi.mail.pec.model.Busta;
import it.dmi.mail.pec.model.DataPEC;
import it.dmi.mail.pec.model.DatiCertificazione;
import it.dmi.mail.pec.model.DestinatarioPEC;
import it.dmi.mail.pec.model.DestinatarioPEC.TipoDestinatario;
import it.dmi.mail.pec.model.ErrorePEC;
import it.dmi.mail.pec.model.Mail;
import it.dmi.mail.pec.model.Mail.DeliveryStatus;
import it.dmi.mail.pec.model.Mail.DeliveryStatus.Action;
import it.dmi.mail.pec.model.Mail.DeliveryStatus.DiagnosticCode;
import it.dmi.mail.pec.model.Mail.DeliveryStatus.TipoStato;
import it.dmi.mail.pec.model.Messaggio;
import it.dmi.mail.pec.model.PEC;
import it.dmi.mail.pec.model.RicevutaPEC;
import it.dmi.mail.pec.model.TipoPostaCert;
import it.dmi.mail.pec.model.TipoRicevuta;
import it.dmi.mail.pec.util.IOUtils;
import it.dmi.mail.pec.util.MimeMessageUtils;
import it.dmi.mail.pec.util.MimeTypesUtil;
import it.dmi.mail.pec.util.PECConstants;
import it.dmi.mail.pec.util.Tupla;
import it.dmi.mail.pec.util.XMLDocumentUtils;
import it.dmi.mail.uudecoder.UUDecoder;
import it.dmi.mail.uudecoder.UUDecoder.UUDecodedAttachment;
import it.dmi.mail.uudecoder.exception.UUDecoderException;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author biagio.tozzi
 *
 */
@Slf4j
public class PECMessageParser {
	
	private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	private Properties properties;

	/**
	 * Istanza di PECMessageParser <br>
	 * L'elaborazione degli oggetti {@link MimeMessage} viene eseguita con le
	 * properties di sistema (System.getProperties())
	 * 
	 * @return PECMessageParser
	 * @throws PECParserException
	 */
	public static PECMessageParser getInstance() throws PECParserException {
		return new PECMessageParser(null);
	}

	/**
	 * Istanza di PECMessageParser <br>
	 * L'elaborazione degli oggetti {@link MimeMessage} viene eseguita con le
	 * properties passate come parametro
	 * 
	 * @param properties
	 * @return PECMessageParser
	 * @throws PECParserException
	 */
	public static PECMessageParser getInstance(Properties properties) throws PECParserException {
		return new PECMessageParser(properties);
	}

	/**
	 * Estrae, a partire da un {@link File} rappresentante l'EML<i>*</i>: <br>
	 * <ul>
	 * <li>Busta di trasporto: {@link Busta}</li>
	 * <li>Eventuale messaggio di Posta Elettronica Certificata: {@link PEC}</li>
	 * <li>Eventuale ricevuta: {@link RicevutaPEC}</li>
	 * </ul>
	 * 
	 * * Il messaggio viene elaborato anche se non PEC:
	 * <ul>
	 * <li>Messaggio ricevuto su una casella di Posta Elettronica Certificata:
	 * l'oggetto {@link PEC} conterrà le informazioni del messaggio normale in
	 * quanto comunque incapsulato in una busta</li>
	 * <li>Messaggio ricevuto su una casella di posta ordinaria: in questo caso la
	 * {@link Busta} rappresenta il messaggio normale</li>
	 * </ul>
	 * 
	 * @param eml
	 * @return
	 * @throws PECParserException
	 */
	public Messaggio parse(File eml) throws PECParserException {

		if (eml == null) {
			throw new PECParserException("Il file è nullo", null);
		}

		if (!eml.exists()) {
			throw new PECParserException("Il file " + eml.getName() + " non esiste", null);
		}

		try (InputStream is = new FileInputStream(eml)) {
			return parse(is);

		} catch (IOException e) {
			log.error("Errore IO: {}", e.getLocalizedMessage(), e);
			throw new PECParserException(null, e);
		}

	}

	private PECMessageParser(Properties properties) throws PECParserException {
		this.properties = properties;
	}
	
	/**
	 * Estrae, a partire da un {@link InputStream} rappresentante l'EML<i>*</i>: <br>
	 * <ul>
	 * <li>Busta di trasporto: {@link Busta}</li>
	 * <li>Eventuale messaggio di Posta Elettronica Certificata: {@link PEC}</li>
	 * <li>Eventuale ricevuta: {@link RicevutaPEC}</li>
	 * </ul>
	 * 
	 * * Il messaggio viene elaborato anche se non PEC:
	 * <ul>
	 * <li>Messaggio ricevuto su una casella di Posta Elettronica Certificata:
	 * l'oggetto {@link PEC} conterrà le informazioni del messaggio normale in
	 * quanto comunque incapsulato in una busta</li>
	 * <li>Messaggio ricevuto su una casella di posta ordinaria: in questo caso la
	 * {@link Busta} rappresenta il messaggio normale</li>
	 * </ul>
	 * 
	 * @param eml
	 * @return
	 * @throws PECParserException
	 */
	public Messaggio parse(InputStream eml) throws PECParserException {
		return parse(MimeMessageUtils.createMimeMessage(eml, properties));
	}

	/**
	 * Estrae, a partire da un {@link MimeMessage} rappresentante un messaggio
	 * PEC<i>*</i>: <br>
	 * <ul>
	 * <li>Busta di trasporto: {@link Busta}</li>
	 * <li>Eventuale messaggio di Posta Elettronica Certificata: {@link PEC}</li>
	 * <li>Eventuale ricevuta: {@link RicevutaPEC}</li>
	 * </ul>
	 * 
	 * * Il messaggio viene elaborato anche se non PEC:
	 * <ul>
	 * <li>Messaggio ricevuto su una casella di Posta Elettronica Certificata:
	 * l'oggetto {@link PEC} conterrà le informazioni del messaggio normale in
	 * quanto comunque incapsulato in una busta</li>
	 * <li>Messaggio ricevuto su una casella di posta ordinaria: in questo caso la
	 * {@link Busta} rappresenta il messaggio normale</li>
	 * </ul>
	 * 
	 * @param mimeMessage
	 * @return
	 * @throws PECParserException
	 */
	public Messaggio parse(MimeMessage mimeMessage) throws PECParserException {
		String xTrasporto = null;
		String xRicevuta = null;
		String xRiferimentoMessageID = null;
		String xTipoRicevuta = null;
		String xVerificaSicurezza = null;

		Messaggio messaggio = new Messaggio();
		Busta busta;

		try {
			xTrasporto = mimeMessage.getHeader(PECConstants.X_TRASPORTO, ",");
			xRicevuta = mimeMessage.getHeader(PECConstants.X_RICEVUTA, ",");
			xRiferimentoMessageID = mimeMessage.getHeader(PECConstants.X_RIFERIMENTO, ",");
			xTipoRicevuta = mimeMessage.getHeader(PECConstants.X_TIPO_RICEVUTA, ",");
			xVerificaSicurezza = mimeMessage.getHeader(PECConstants.X_VERIFICA_SICUREZZA, ",");

		} catch (MessagingException e) {
			log.error("Errore durante la lettura degli header di trasporto", e);
			throw new PECParserException("Errore durante la lettura degli header di trasporto", e);
		}

		try {
			busta = estraiBusta(mimeMessage, xTrasporto != null || xRicevuta != null);
			busta.setXRicevuta(xRicevuta);
			busta.setXRiferimentoMessageID(xRiferimentoMessageID);
			busta.setXTrasporto(xTrasporto);
			busta.setXTipoRicevuta(xTipoRicevuta);
			busta.setXVerificaSicurezza(xVerificaSicurezza);

		} catch (PECParserException e) {
			log.error("Errore durante l'elaborazione della busta");
			throw e;
		}

		messaggio.setBusta(busta);

		if (xTrasporto != null) {

			try {
				messaggio.setPec(estraiPEC(busta, xTrasporto));

			} catch (PECParserException e) {
				log.error("Errore durante l'elaborazione della PEC");
				throw e;
			}

		} else if (xRicevuta != null) {

			try {
				messaggio.setRicevuta(estraiRicevuta(busta));

			} catch (PECParserException e) {
				log.error("Errore durante l'elaborazione della ricevuta");
				throw e;
			}
		}

		return messaggio;
	}

	private static Busta estraiBusta(MimeMessage mimeMessage, boolean elaboraPEC) throws PECParserException {
		Busta busta = new Busta();
		Tupla<DataSource, DataSource> tupla = new Tupla<DataSource, DataSource>();
		parse(mimeMessage, busta, tupla, elaboraPEC);
		busta.setPostaCert(tupla.getElementA());
		busta.setDatiCert(tupla.getElementB());
		return busta;
	}

	private RicevutaPEC estraiRicevuta(Busta busta) throws PECParserException {

		try {
			RicevutaPEC ricevutaPEC = new RicevutaPEC();
			ricevutaPEC.setDatiCertificazione(estraiDatiCertificazione(busta.getDatiCert().getInputStream()));

			if (busta.getPostaCert() != null) {
				PEC pec = estraiPEC(MimeMessageUtils.createMimeMessage(busta.getPostaCert().getInputStream(), this.properties));
				ricevutaPEC.setMessaggioOriginale(pec);
			}

			return ricevutaPEC;

		} catch (Exception e) {
			throw new PECParserException("Errore durante l'elaborazione della ricevuta", e);
		}

	}

	private PEC estraiPEC(Busta busta, String xTrasporto) throws PECParserException {

		try {
			MimeMessage postaCertMimeMessage = MimeMessageUtils.createMimeMessage(busta.getPostaCert().getInputStream(), this.properties);
			PEC pec = estraiPEC(postaCertMimeMessage);
			
			if (busta.getDatiCert() != null && (TipoPostaCert.POSTA_CERTIFICATA.getDescrizione().equals(xTrasporto) || TipoPostaCert.ERRORE.getDescrizione().equals(xTrasporto))) {
				pec.setDatiCertificazione(estraiDatiCertificazione(busta.getDatiCert().getInputStream()));
			}
			
			return pec;

		} catch (Exception e) {
			throw new PECParserException("Errore durante l'elaborazione della PEC", e);
		}

	}

	private static PEC estraiPEC(MimeMessage postaCertMimeMessage) throws PECParserException {
		PEC pec = new PEC();
		parse(postaCertMimeMessage, pec, null, false);
		return pec;
	}

	private DatiCertificazione estraiDatiCertificazione(InputStream inputStream) throws PECParserException {
		Document datiCertDocument;
		try {
			datiCertDocument = documentBuilderFactory.newDocumentBuilder().parse(inputStream);

		} catch (SAXException | IOException e) {
			log.error("Errore durante il parsing del daticert.xml", e);
			throw new PECParserException("Errore durante il parsing del daticert.xml", e);

		} catch (ParserConfigurationException e) {
			log.error("Errore durante l'inizializzazione del DocumentBuilder XML", e);
			throw new PECParserException("Errore durante l'inizializzazione del DocumentBuilder XML", e);
		}

		DatiCertificazione datiCertificazione = new DatiCertificazione();
		datiCertificazione.setMittente(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_MITTENTE_PATH, false));
		Map<String, String> destinatari = XMLDocumentUtils.getTextAndAttribute(datiCertDocument,
				PECConstants.DATICERT_DESTINATARI_PATH, PECConstants.DATICERT_DESTINATARI_TIPO_ATTRIBUTE, false);
		for (Map.Entry<String, String> entry : destinatari.entrySet()) {
			datiCertificazione.getDestinatari().add(new DestinatarioPEC(entry.getKey(), TipoDestinatario.from(entry.getValue())));
		}

		datiCertificazione.setRisposte(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_RISPOSTE_PATH, false));
		datiCertificazione.setOggetto(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_OGGETTO_PATH, true));
		datiCertificazione.setGestoreEmittente(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_GESTORE_EMITTENTE_PATH, false));
		datiCertificazione.setData(new DataPEC(
				XMLDocumentUtils.getAttribute(datiCertDocument, PECConstants.DATICERT_DATA_PATH,
						PECConstants.DATICERT_DATA_ZONA_ATTRIBUTE, false),
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_DATA_GIORNO_PATH, false),
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_DATA_ORA_PATH, false)));
		datiCertificazione.setTipoRicevuta(TipoRicevuta.from(XMLDocumentUtils.getAttribute(datiCertDocument,
				PECConstants.DATICERT_RICEVUTA_PATH, PECConstants.DATICERT_RICEVUTA_TIPO_ATTRIBUTE, true)));
		datiCertificazione.setErrore(ErrorePEC.from(XMLDocumentUtils.getAttribute(datiCertDocument,
				PECConstants.DATICERT_POSTACERT_PATH, PECConstants.DATICERT_POSTACERT_ERRORE_ATTRIBUTE, false)));
		datiCertificazione.setErroreEsteso(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_ERRORE_ESTESO_PATH, true));
		datiCertificazione.setConsegna(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_CONSEGNA_PATH, true));
		datiCertificazione.setRicezione(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_RICEZIONE_PATH, true));
		datiCertificazione.setMessageID(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_MESSAGE_ID_PATH, true));
		datiCertificazione.setIdentificativo(
				XMLDocumentUtils.getTextContent(datiCertDocument, PECConstants.DATICERT_IDENTIFICATIVO_PATH, true));
		datiCertificazione.setTipo(TipoPostaCert.from(XMLDocumentUtils.getAttribute(datiCertDocument,
				PECConstants.DATICERT_POSTACERT_PATH, PECConstants.DATICERT_IDENTIFICATIVO_TIPO_ATTRIBUTE, false)));
		return datiCertificazione;
	}

	private static void parse(MimeMessage mimeMessage, Mail mail, Tupla<DataSource, DataSource> tupla,
			boolean elaboraPEC) throws PECParserException {

		try {
			Address[] mittenti = mimeMessage.getFrom();
			if (mittenti != null) {
				for (Address m : mittenti) {
					if (m != null && m instanceof InternetAddress) {
						String address = ((InternetAddress) m).getAddress();
						if (!mail.getMittenti().contains(address)) {
							mail.getMittenti().add(address);
						}
					}
				}
			}

		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione dei mittenti della mail", e);
			throw new PECParserException("Errore durante l'elaborazione dei mittenti della mail", e);
		}

		try {
			Address[] destinatari = mimeMessage.getRecipients(RecipientType.TO);			
			if (destinatari != null) {
				for (Address m : destinatari) {
					if (m != null && m instanceof InternetAddress) {
						String address = ((InternetAddress) m).getAddress();
						if (!mail.getDestinatari().contains(address)) {
							mail.getDestinatari().add(address);
						}
					}
				}

			}

		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione dei destinatari della mail", e);
			throw new PECParserException("Errore durante l'elaborazione dei destinatari della mail", e);
		}

		try {
			Address[] destinatari = mimeMessage.getRecipients(RecipientType.CC);
			if (destinatari != null) {
				for (Address m : destinatari) {
					if (m != null && m instanceof InternetAddress) {
						String address = ((InternetAddress) m).getAddress();
						if (!mail.getDestinatariCopiaConoscenza().contains(address)) {
							mail.getDestinatariCopiaConoscenza().add(address);
						}
					}
				}
			}

		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione dei destinatari in copia conoscenza della mail", e);
			throw new PECParserException("Errore durante l'elaborazione dei destinatari in copia conoscenza della mail",
					e);
		}

		try {
			Address[] destinatari = mimeMessage.getRecipients(RecipientType.BCC);
			if (destinatari != null) {
				for (Address m : destinatari) {
					if (m != null && m instanceof InternetAddress) {
						String address = ((InternetAddress) m).getAddress();
						if (!mail.getDestinatariCopiaConoscenzaNascosta().contains(address)) {
							mail.getDestinatariCopiaConoscenzaNascosta().add(address);
						}
					}
				}

			}

		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione dei destinatari in copia conoscenza nascosta della mail", e);
			throw new PECParserException(
					"Errore durante l'elaborazione dei destinatari in copia conoscenza nascosta della mail", e);
		}

		try {
			mail.setDataInvio(mimeMessage.getSentDate());

		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione della data di invio della mail", e);
			throw new PECParserException("Errore durante l'elaborazione della data di invio della mail", e);
		}

		try {
			mail.setDataRicezione(mimeMessage.getReceivedDate());

		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione della data di ricezione della mail", e);
			throw new PECParserException("Errore durante l'elaborazione della data di ricezione della mail", e);
		}

		try {
			String oggetto = mimeMessage.getSubject();

			if (oggetto != null) {
				mail.setOggetto(MimeUtility.decodeText(oggetto));
			}

		} catch (MessagingException | UnsupportedEncodingException e) {
			log.error("Errore durante l'elaborazione dell'oggetto della mail", e);
			throw new PECParserException("Errore durante l'elaborazione dell'oggetto della mail", e);
		}

		try {
			String messageID = null;
			
			if ((messageID = mimeMessage.getMessageID()) != null) {
				messageID = messageID.replaceAll("<", "").replaceAll(">", "");
			}
			
			if (messageID == null || messageID.isEmpty()) {
				// Alcuni messaggi di posta ordinaria non hanno un messageID
				messageID = MimeMessageUtils.getUniqueMessageID(mimeMessage);
			}
			
			mail.setMessageID(messageID);

		} catch (PECParserException e) {
			log.error("Errore durante l'elaborazione del messageID", e);
			throw e;
			
		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione del messageID", e);
			throw new PECParserException("Errore durante l'elaborazione del messageID della mail", e);
		}

		try {
			String[] inReplyTO = mimeMessage.getHeader(PECConstants.IN_REPLY_TO);
			if (inReplyTO != null && inReplyTO.length > 0) {
				List<String> inReplyToList = new ArrayList<String>();
				inReplyToList.add(inReplyTO[0]);
				mail.setReplyToMessageID(inReplyToList.get(0).replaceAll("<", "").replaceAll(">", ""));
			}


		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione del reply to", e);
			throw new PECParserException("Errore durante l'elaborazione del reply to della mail", e);
		}
		
		try {
			String[] referencesHeader = mimeMessage.getHeader(PECConstants.REFERENCES);
			if (referencesHeader != null && referencesHeader.length > 0) {
				String[] referencesArray = referencesHeader[0].split(" ");
				List<String> referencesList = new ArrayList<String>();
				for (String r : referencesArray) {
					referencesList.add(r.replaceAll("<", "").replaceAll(">", ""));
				}
				mail.getReplyToHistoryMessagesID().addAll(referencesList);
			}


		} catch (MessagingException e) {
			log.error("Errore durante l'elaborazione del reply to", e);
			throw new PECParserException("Errore durante l'elaborazione del reply to della mail", e);
		}
		
		parseContent(mimeMessage, mail, tupla, elaboraPEC);
	}

	private static void parseContent(MimePart part, Mail mail, Tupla<DataSource, DataSource> tupla, boolean elaboraPEC)
			throws PECParserException {

		Object content = MimeMessageUtils.getContent(part);

		if (MimeMessageUtils.isMimeType(part, MimeTypesUtil.CONTENT_TYPE_TEXT_PLAIN)
				&& !Part.ATTACHMENT.equalsIgnoreCase(MimeMessageUtils.getDisposition(part))
				&& mail.getCorpoTesto() == null) {

			String corpo = content.toString();

			try {
				if (UUDecoder.containsUUEncodedAttachments(content.toString())) {

					corpo = corpo.substring(0, UUDecoder.getNextBeginIndex(corpo));
					List<UUDecodedAttachment> decodedAttachments = null;

					try {
						decodedAttachments = UUDecoder.getUUDecodedAttachments(content.toString());

					} catch (UUDecoderException e) {
						log.error("Errore durante l'estrazione degli allegati codificati in uuencoding", e);
						throw new PECParserException(
								"Errore durante l'estrazione degli allegati codificati in uuencoding", e);
					}

					for (UUDecodedAttachment uda : decodedAttachments) {
						mail.getAllegati().add(new Allegato(uda.getFileName(), uda.getDataSource(), null, null, false));
					}
				}

			} catch (UUDecoderException e) {
				log.error("Errore durante la verifica di allegati codificati in uuencoding", e);
				throw new PECParserException("Errore durante la verifica di allegati codificati in uuencoding", e);
			}

			mail.setCorpoTesto(MimeMessageUtils.decodeText(corpo));

		} else {

			if (MimeMessageUtils.isMimeType(part, MimeTypesUtil.CONTENT_TYPE_TEXT_HTML)
					&& !Part.ATTACHMENT.equalsIgnoreCase(MimeMessageUtils.getDisposition(part))) {

				if (mail.getCorpoHTML() == null) {
					mail.setCorpoHTML(MimeMessageUtils.decodeText(content.toString()));

				} else {
					mail.setCorpoHTML(mail.getCorpoHTML() + MimeMessageUtils.decodeText(content.toString()));
				}

			} else {

				if (MimeMessageUtils.isMimeType(part, MimeTypesUtil.CONTENT_TYPE_MULTIPART)) {
					Multipart multipart = (Multipart) content;

					for (int i = 0; i < MimeMessageUtils.getCount(multipart); i++) {
						parseContent(((MimeBodyPart) MimeMessageUtils.getBodyPart(multipart, i)), mail, tupla,
								elaboraPEC);
					}

				} else if (MimeMessageUtils.isMimeType(part, MimeTypesUtil.CONTENT_TYPE_DELIVERY_STATUS)) {
					
					mail.setDeliveryStatus(true);
					DeliveryStatus deliveryStatus = new DeliveryStatus();

					try(BufferedReader br = new BufferedReader(new InputStreamReader(part.getInputStream()))) {
						
						String line = br.readLine();
						
						while (line != null) {
							
							if (line.toLowerCase().startsWith(PECConstants.DELIVERY_ACTION.toLowerCase() + ":")) {
								String action = line.substring(PECConstants.DELIVERY_ACTION.length() + 1).trim();
								deliveryStatus.setAction(Action.from(action));

							} else if (line.toLowerCase().startsWith(PECConstants.DELIVERY_STATUS.toLowerCase() + ":")) {
								String status = line.substring(PECConstants.DELIVERY_STATUS.length() + 1).trim();
								deliveryStatus.setStatus(status);

								if (status != null && !status.isEmpty()) {
									char first = status.charAt(0);
									int prefix = Character.getNumericValue(first);
									
									if (prefix > 0) {
										deliveryStatus.setTipoStato(TipoStato.from(prefix));
									}
								}
								
							} else if (line.toLowerCase().startsWith(PECConstants.DELIVERY_DIAGNOSTIC_CODE.toLowerCase() + ":")) {
								String diagnosticCode = line.substring(PECConstants.DELIVERY_DIAGNOSTIC_CODE.length() + 1).trim();
								
								DiagnosticCode dc = new DiagnosticCode();
								if (diagnosticCode.contains(";")) {
									dc.setType(diagnosticCode.substring(0, diagnosticCode.indexOf(";")));
									dc.setDescription(diagnosticCode.substring(diagnosticCode.indexOf(";") + 1));
									
								} else {
									dc.setDescription(diagnosticCode);
								}
								
								deliveryStatus.setDiagnosticCode(dc);
								
							} else if (line.toLowerCase().startsWith(PECConstants.DELIVERY_REMOTE_MTA.toLowerCase() + ":")) {
								String remoteMta = line.substring(PECConstants.DELIVERY_REMOTE_MTA.length() + 1).trim();
								deliveryStatus.setRemoteMTA(remoteMta);
								
							} else if (line.toLowerCase().startsWith(PECConstants.DELIVERY_REPORTING_MTA.toLowerCase() + ":")) {
								String reportingMTA = line.substring(PECConstants.DELIVERY_REPORTING_MTA.length() + 1).trim();
								deliveryStatus.setReportingMTA(reportingMTA);
								
							} else if (line.toLowerCase().startsWith(PECConstants.DELIVERY_RECEIVED_FROM_MTA.toLowerCase() + ":")) {
								String recivedFromMTA = line.substring(PECConstants.DELIVERY_RECEIVED_FROM_MTA.length() + 1).trim();
								deliveryStatus.setReceivedFromMTA(recivedFromMTA);
								
							} else if (line.toLowerCase().startsWith(PECConstants.DELIVERY_FINAL_RECIPIENT.toLowerCase() + ":")) {
								String finalRecipient = line.substring(PECConstants.DELIVERY_FINAL_RECIPIENT.length() + 1).trim();
								deliveryStatus.setFinalRecipient(finalRecipient);
							}
							
							line = br.readLine();
						}
						
						
					} catch (Exception e) {
						log.error("Errore durante il parsing degli header di delivery status", e);
					}
					
					mail.setDeliveryStatusInfo(deliveryStatus);
					
				} else {

					boolean pec = false;

					if (elaboraPEC && tupla != null && !tupla.isComplete()) {
						if (MimeMessageUtils.getDisposition(part) != null
								&& MimeMessageUtils.getFileName(part) != null) {

							if (tupla.getElementA() == null && (PECConstants.POSTACERT_EML_NAME
									.equalsIgnoreCase(MimeMessageUtils.decodeText(MimeMessageUtils.getFileName(part)))
									|| PECConstants.POSTACERT_EML_NAME.equals(MimeMessageUtils.getFileName(part)))

									&& MimeMessageUtils.isMimeType(part, MimeTypesUtil.CONTENT_TYPE_MESSAGE_RFC822)) {

								pec = true;

								try {
									tupla.setElementA(IOUtils.createDataSource(part));

								} catch (PECParserException | IOException e) {
									log.error("Errore durante l'estrazione del file {}",
											PECConstants.POSTACERT_EML_NAME, e);
									throw new PECParserException(
											"Errore durante l'estrazione del file " + PECConstants.POSTACERT_EML_NAME,
											e);
								}

							} else if (tupla.getElementB() == null && (PECConstants.DATICERT_XML_NAME
									.equalsIgnoreCase(MimeMessageUtils.decodeText(MimeMessageUtils.getFileName(part)))
									|| PECConstants.DATICERT_XML_NAME.equals(MimeMessageUtils.getFileName(part)))
									&& MimeMessageUtils.isMimeType(part, MimeTypesUtil.CONTENT_TYPE_APPLICATION_XML)) {

								pec = true;

								try {
									tupla.setElementB(IOUtils.createDataSource(part));

								} catch (PECParserException | IOException e) {
									log.error("Errore durante l'estrazione del file {}",
											PECConstants.DATICERT_XML_NAME, e);
									throw new PECParserException(
											"Errore durante l'estrazione del file " + PECConstants.DATICERT_XML_NAME,
											e);
								}
							}
						}
					}

					if (!pec) {
						DataSource dataSource;

						try {
							dataSource = IOUtils.createDataSource(part);

						} catch (PECParserException | IOException e) {
							log.error("Errore durante l'estrazione dell'allegato {}",
									MimeMessageUtils.getFileName(part), e);
							throw new PECParserException(
									"Errore durante l'estrazione dell'allegato " + MimeMessageUtils.getFileName(part),
									e);
						}

						mail.getAllegati()
								.add(new Allegato(dataSource.getName(), dataSource,
										MimeMessageUtils.getHeaderValue(PECConstants.CONTENT_ID, part),
										MimeMessageUtils.getHeaderValue(PECConstants.X_ATTACHMENT_ID, part),
										Part.INLINE.equals(MimeMessageUtils.getDisposition(part))));
					}
				}
			}
		}
	}

}
