package hashkitty.java.attack;

import com.google.gson.annotations.SerializedName;

/**
 * Data Transfer Object representing the attack parameters received from a remote client.
 * This class maps to the JSON payload structure sent by the Android application.
 */
public class AttackParams {
    @SerializedName("job_id")
    public String jobId;

    public String file;

    public String mode;

    @SerializedName("attack_mode")
    public String attackMode;

    public String wordlist;

    public String rules;
}
