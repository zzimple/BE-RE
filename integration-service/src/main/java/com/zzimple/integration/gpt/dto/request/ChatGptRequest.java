package com.zzimple.integration.gpt.dto.request;

import com.zzimple.integration.gpt.dto.EstimateDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatGptRequest {
  private EstimateDto estimateA;
  private EstimateDto estimateB;
}