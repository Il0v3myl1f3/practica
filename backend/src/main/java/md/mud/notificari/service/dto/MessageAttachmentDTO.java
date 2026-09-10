package md.mud.notificari.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link md.mud.notificari.domain.MessageAttachment} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MessageAttachmentDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String fileName;

    @Size(max = 100)
    private String contentType;

    @Lob
    private byte[] file;

    private String fileContentType;

    @NotNull
    private MessageDTO message;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public String getFileContentType() {
        return fileContentType;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }

    public MessageDTO getMessage() {
        return message;
    }

    public void setMessage(MessageDTO message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageAttachmentDTO)) {
            return false;
        }

        MessageAttachmentDTO messageAttachmentDTO = (MessageAttachmentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, messageAttachmentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MessageAttachmentDTO{" +
            "id=" + getId() +
            ", fileName='" + getFileName() + "'" +
            ", contentType='" + getContentType() + "'" +
            ", file='" + getFile() + "'" +
            ", message=" + getMessage() +
            "}";
    }
}
