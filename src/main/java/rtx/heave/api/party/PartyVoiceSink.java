package rtx.heave.api.party;

@FunctionalInterface
public interface PartyVoiceSink {
    void accept(String username, int seq, byte[] opusData);
}