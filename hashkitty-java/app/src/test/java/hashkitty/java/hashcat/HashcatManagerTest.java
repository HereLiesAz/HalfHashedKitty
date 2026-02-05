package hashkitty.java.hashcat;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HashcatManagerTest {

    @Test
    void buildCommand_DictionaryAttack_NoAdvanced() {
        HashcatManager manager = new HashcatManager((s) -> {}, (s) -> {}, () -> {});
        List<String> cmd = manager.buildCommand("0", "Dictionary", null, false, false, null);

        // Expected: hashcat -m 0 -a 0 --status --status-timer=5
        // Note: Indices may shift if order changes, but testing core components.
        // 0: hashcat
        // 1: -m
        // 2: 0
        // 3: -a
        // 4: 0
        // 5: --status
        // 6: --status-timer=5

        assertEquals("hashcat", cmd.get(0));
        assertEquals("-m", cmd.get(1));
        assertEquals("0", cmd.get(2));
        assertEquals("-a", cmd.get(3));
        assertEquals("0", cmd.get(4));
        assertEquals("--status", cmd.get(5));
        assertEquals("--status-timer=5", cmd.get(6));
        assertEquals(7, cmd.size());
    }

    @Test
    void buildCommand_MaskAttack_AllAdvanced() {
        HashcatManager manager = new HashcatManager((s) -> {}, (s) -> {}, () -> {});
        List<String> cmd = manager.buildCommand("22000", "Mask", "rules.txt", true, true, "3");

        // Expected includes: --force, -O, -w 3, -a 3, -r rules.txt, --status...
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
