package eu.asylum.common.utils;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class NekobinUploader {

    private static URI uri = null;


    @SneakyThrows
    public static CompletableFuture<NekobinResult> upload(@NonNull String content) {

        if (uri == null) uri = new URI("https://nekobin.com/api/documents");

        var o = new JsonObject();
        o.addProperty("content", content);
        var contentJson = Constants.get().getGson().toJson(o);
        var httpRequest = HttpRequest.newBuilder(NekobinUploader.uri)
                .POST(HttpRequest.BodyPublishers.ofString(contentJson))
                .setHeader("Content-Type", "application/json")
                .build();

        return Constants.get()
                .getHttpClient()
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(body -> Constants.get().getGson().fromJson(body, NekobinResult.class))
                .toCompletableFuture();
    }

    @Getter
    @ToString
    public static final class NekobinDocument {
        @SerializedName("key")
        private final String key = "";
        @SerializedName("title")
        private final String title = "";
        @SerializedName("author")
        private final String author = "";
        @SerializedName("date")
        private final String date = "";
        @SerializedName("views")
        private final int views = -1;
        @SerializedName("length")
        private final int length = -1;
        @SerializedName("content")
        private final String content = "";

        public String asUrl() {
            return "https://nekobin.com/" + this.key;
        }
    }

    @Getter
    @Setter
    @ToString
    public static final class NekobinResult {
        @SerializedName("ok")
        private boolean ok;
        @SerializedName("result")
        private NekobinDocument document;
        @SerializedName("error")
        private String error;
    }

}
