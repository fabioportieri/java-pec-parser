package it.dmi.mail.uudecoder.exception.model;


import java.util.ArrayList;
import java.util.List;

import it.dmi.mail.uudecoder.UUDecoder.UUDecodedAttachment;
import lombok.Data;

/**
 *
 * @author biagio.tozzi
 *
 */
@Data
public class UUDecodedBean {

    private String content;
    private List<UUDecodedAttachment> attachments = new ArrayList<>();

}