package eu.asylum.common.utils;

import com.google.gson.annotations.SerializedName;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;

public class UuidConverter {

  UuidConverter() {}

  @SneakyThrows
  public static CompletableFuture<MinecraftProfile> getUUID(@NonNull String username) {
    HttpRequest httpRequest =
        HttpRequest.newBuilder(
                new URI("https://api.mojang.com/users/profiles/minecraft/" + username))
            .GET()
            .build();
    return Constants.get()
        .getHttpClient()
        .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
        .thenApply(HttpResponse::body)
        .thenApply(body -> Constants.get().getGson().fromJson(body, MinecraftProfile.class))
        .toCompletableFuture();
  }

  @EqualsAndHashCode
  @Getter
  @Setter
  @ToString
  public static class MinecraftProfile {

    @SerializedName("name")
    private String username;

    @SerializedName("id")
    private String uuid;

    public UUID getAsUUID() {
      return UUID.fromString(
          uuid.replaceFirst(
              "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
              "$1-$2-$3-$4-$5"));
    }

    public String asJson() {
      return Constants.get().getGson().toJson(this);
    }
  }
}
