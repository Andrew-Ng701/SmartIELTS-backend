package com.andrew.smartielts;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SmartIeltsApplicationTests {

    @Test
    void main()  {
        String apiKey = "sk-f9e0daea76924a8096470fa1e337e105";
        String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

        String json = """
                {
                  "model": "qwen3.5-flash",
                  "messages": [{"role": "user", "content": "tell about react,i learned vue"}],
                  "enable_thinking": false
                }
                """;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(3600, TimeUnit.SECONDS) // 必须 ≥ 服务端最大处理时间
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            System.out.println("HTTP " + response.code());
            System.out.println(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
