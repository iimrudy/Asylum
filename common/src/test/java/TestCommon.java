import eu.asylum.common.utils.IpInfo;
import eu.asylum.common.utils.NekobinUploader;
import eu.asylum.common.utils.Serialization;
import eu.asylum.common.utils.UuidConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class TestCommon {


   /* public static void main(String[] args) throws Exception {


        var current = InetAddress.getByName("1.1.1.1");
        IpInfo.fetchIp(current).thenAccept(System.out::println);

        ArrayList<FakePlayer> onlinePlayers = new ArrayList<>();
        addFakePlayer("iim_rudy", onlinePlayers);
        addFakePlayer("Guntherman_3", onlinePlayers);
        addFakePlayer("Hypixel", onlinePlayers);


        AsylumProvider<FakePlayer> asylumProvider = new AsylumProvider<FakePlayer>() {
            @Override
            public List<FakePlayer> getOnlinePlayers() {
                return onlinePlayers;
            }

            @Override
            public UUID getUUID(FakePlayer s) {
                return s.getAsUUID();
            }

            @Override
            public String getUsername(FakePlayer s) {
                return s.getUsername();
            }

            @Override
            public boolean isOnline(FakePlayer s) {
                return false;
            }
        };
        Thread.sleep(2000);

        for (FakePlayer fp : onlinePlayers) {
            asylumProvider
                    .getAsylumPlayerAsync(fp)
                    .thenAccept(
                            (z) -> z.ifPresentOrElse((x) -> System.out.println("AGGIUNGO player di merda   " + x.getUsername()),
                                    () -> System.out.println("cacca")));
        }
        for (int i = 0; i < 10; i++) {
            for (FakePlayer fp : onlinePlayers) {
                asylumProvider
                        .getAsylumPlayerAsync(fp)
                        .thenAccept(
                                (z) -> z.ifPresentOrElse((x) -> System.out.println("playerDiMerda   " + x.getUsername()),
                                        () -> System.out.println("nonAggiuntoPlayerDiMerda")));
            }
        }

        asylumProvider.getAsylumPlayer(onlinePlayers.get(0))
                .ifPresentOrElse(
                        (x) -> System.out.println("aaaaaaaaaaAddiungo player di merda   " + x.getUsername()),
                        () -> System.out.println("noasdasdasdsadn aggiuntoplayer di merda"));


        while (true) ;
    }*/

    public static void addFakePlayer(String user, List<FakePlayer> fplist) {
        UuidConverter.getUUID(user).thenAccept(account -> {
            FakePlayer fp = new FakePlayer();
            fp.setUsername(account.getUsername());
            fp.setUuid(account.getUuid());
            fplist.add(fp);
        });
    }

    private static long countLines(File start, String ext) {
        long count = 0;
        for (File f : start.listFiles()) {
            if (f.isDirectory()) {
                count += countLines(f, ext); // recursive go brr
            } else {
                try {
                    if (f.getName().endsWith(ext)) {
                        count += Files.readAllLines(f.toPath()).size();
                    }
                } catch (Exception e) {
                    // ignored it's fine
                }
            }
        }
        return count;
    }

    @Test
    public void testIpInfo() {
        Assertions.assertDoesNotThrow(() -> {
            var ip = "1.1.1.1";
            var current = InetAddress.getByName(ip);
            var response = IpInfo.fetchIp(current).get();
            Assertions.assertEquals(response.getIp(), ip);
        });
    }

    @Test
    public void testNekoBin() {
        Assertions.assertDoesNotThrow(() -> {
            var response = NekobinUploader.upload("Some Text to Upload").get();
            Assertions.assertTrue(response.isOk());
            Assertions.assertNull(response.getError());
            Assertions.assertNotNull(response.getDocument());
            Assertions.assertEquals("Some Text to Upload", response.getDocument().getContent());
        });
    }

    @Test
    public void lineCounter() {
        long len = countLines(new File("../"), ".java");
        System.out.println("Total lines (.java): " + len);
    }

    @Test
    public void SerializationTests() throws Exception {
        Data d = new Data();
        List<String> a = new ArrayList<>();
        IntStream.range(0, 500).forEach(i -> a.add("aaa" + i));
        d.o = a;
        var s1 = Serialization.serialize(d);
        var s2 = Serialization.serializeCompressed(d);
        var d1 = (Data) Serialization.deserialize(s1);
        var d2 = (Data) Serialization.deserializeCompressed(s2);
    }

    private static class FakePlayer extends UuidConverter.MinecraftProfile {

    }

    private static class Data implements Serializable {
        Object o;
    }

}
