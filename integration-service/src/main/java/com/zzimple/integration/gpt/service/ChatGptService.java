package com.zzimple.integration.gpt.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzimple.integration.gpt.dto.Body;
import com.zzimple.integration.gpt.dto.Message;
import com.zzimple.integration.gpt.dto.request.ChatGptRequest;
import com.zzimple.integration.gpt.dto.response.ChatGptResponse;
import com.zzimple.integration.gpt.format.GptSystemPromptFormatter;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ChatGptService {

  @Value("${openai.api.key}")
  private String key;
  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ChatGptResponse getChatGptResponse(ChatGptRequest request) {
    String prompt = GptSystemPromptFormatter.buildPrompt(request);

    URI uri = UriComponentsBuilder
        .fromUriString("https://api.openai.com/v1/chat/completions")
        .build()
        .encode()
        .toUri();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(key);

    List<Message> messages = List.of(new Message("user", prompt));
    Body body = new Body("gpt-4o", messages);

    RequestEntity<?> httpRequest = new RequestEntity<>(body, headers, HttpMethod.POST, uri);
    ResponseEntity<String> response = restTemplate.exchange(httpRequest, String.class);

    String rawBody = response.getBody();
    System.out.println("🔍 OpenAI Raw Response:\n" + rawBody);

    try {
      // content 자체가 JSON 형식이므로 EstimateCompareResponse로 매핑
      String content = objectMapper
          .readTree(response.getBody())
          .path("choices").get(0)
          .path("message")
          .path("content").asText();

      content = content
          .replaceAll("(?m)^```(?:json)?\\s*", "")   // 시작 백틱 제거
          .replaceAll("(?m)```\\s*$", "")           // 끝 백틱 제거
          .trim();

      System.out.println("GPT 응답:\n" + content); // 🔍 이거 찍어서 실제 응답 확인

      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


      return objectMapper.readValue(content, ChatGptResponse.class);

    } catch (Exception e) {
      throw new RuntimeException("GPT 응답 파싱 오류. content='" + rawBody + "'", e);
    }
  }
}

