package hashkitty.java.hashcat;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HashcatManagerTest {

    @Test
    void buildCommand_DictionaryAttack_NoAdvanced() {
        HashcatManager manager = new HashcatManager(null, null, null);
        List<String> cmd = manager.buildCommand("0", "Dictionary", null, false, false, null);

        // hashcat -m 0 --potfile-disable -a 0
        assertEquals("hashcat", cmd.get(0));
        assertEquals("-m", cmd.get(1));
        assertEquals("0", cmd.get(2));
        assertEquals("--potfile-disable", cmd.get(3));
        assertEquals("-a", cmd.get(4));
        assertEquals("0", cmd.get(5));
        assertEquals(6, cmd.size());
    }

    @Test
    void buildCommand_MaskAttack_AllAdvanced() {
        HashcatManager manager = new HashcatManager(null, null, null);
        List<String> cmd = manager.buildCommand("22000", "Mask", "rules.txt", true, true, "3");

        // hashcat -m 22000 --potfile-disable --force -O -w 3 -a 3 -r rules.txt
        assertTrue(cmd.contains("--force"));
        assertTrue(cmd.contains("-O"));

        int wIndex = cmd.indexOf("-w");
        assertTrue(wIndex > 0);
        assertEquals("3", cmd.get(wIndex + 1));

        int aIndex = cmd.indexOf("-a");
        assertTrue(aIndex > 0);
        assertEquals("3", cmd.get(aIndex + 1));

        int rIndex = cmd.indexOf("-r");
        assertTrue(rIndex > 0);
        assertEquals("rules.txt", cmd.get(rIndex + 1));
    }
}
