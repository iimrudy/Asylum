import eu.asylum.common.utils.IpInfo;
import eu.asylum.common.utils.NekobinUploader;
import eu.asylum.common.utils.UuidConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;

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

    @Test
    public void testIpInfo() throws Exception {
        var ip = "1.1.1.1";
        var current = InetAddress.getByName(ip);

        Assertions.assertDoesNotThrow(() -> {
            var response = IpInfo.fetchIp(current).get();
            Assertions.assertEquals(response.getIp(), ip);
        });
    }

    @Test
    public void testNekoBin() throws Exception {
        System.out.println(NekobinUploader.upload("SOME TEXT TO UPLOAD LOLOLOLOLO").get());
    }

    private static class FakePlayer extends UuidConverter.MinecraftProfile {

    }

}
