package ksh.tryptobackend.transfer.domain.vo;

public record TransferDestinationChain(boolean tagRequired) {

    public boolean isMissingRequiredTag(String tag) {
        return tagRequired && (tag == null || tag.isBlank());
    }
}
