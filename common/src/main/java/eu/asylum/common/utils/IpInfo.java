package eu.asylum.common.utils;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class IpInfo {

    @SneakyThrows
    public static CompletableFuture<IpInfo.GeoIpResponse> fetchIp(@NonNull InetAddress address) {
        String url = "https://freegeoip.app/json/" + address.getHostAddress();
        HttpRequest httpRequest = HttpRequest.newBuilder(new URI(url)).GET().build();
        return Constants
                .get()
                .getHttpClient()
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(ss -> {
                    if (ss.equals("404 page not found")) {
                        return "{}";
                    }
                    return ss;
                })
                .thenApply(body -> Constants.get().getGson().fromJson(body, IpInfo.GeoIpResponse.class));
    }

    @Getter
    @EqualsAndHashCode
    public final class GeoIpResponse {

        @SerializedName("ip")
        private String ip;
        @SerializedName("country_code")
        private String countryCode;
        @SerializedName("country_name")
        private String countryName;
        @SerializedName("region_code")
        private String regionCode;
        @SerializedName("region_name")
        private String regionName;
        @SerializedName("city")
        private String city;
        @SerializedName("zip_code")
        private String zipCode;
        @SerializedName("time_zone")
        private String timeZone;
        @SerializedName("latitude")
        private String latitude;
        @SerializedName("longitude")
        private String longitude;
        @SerializedName("metro_code")
        private String metroCode;

        @Override
        public String toString() {
            return Constants.get().getGson().toJson(this);
        }

    }

}
