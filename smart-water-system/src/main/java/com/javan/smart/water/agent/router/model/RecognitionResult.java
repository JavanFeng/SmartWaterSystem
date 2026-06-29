/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.javan.smart.water.agent.router.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.javan.smart.water.common.enums.IntentEnum;
import lombok.Data;

/**
 * 识别结果
 * @author Javan
 * @since 1.0.0
 */
@Data
public class RecognitionResult {

	/**
	 * ID
	 */
	private String recognitionName;

	/**
	 * Status
	 */
	private RecognitionStatus status;

	/**
	 *  intent
	 */
	private IntentEnum intent;

	/**
	 * Raw prompt sent to LLM (for observability)
	 */
	private String rawPrompt;

	/**
	 * Error message if status is ERROR
	 */
	private String errorMessage;

	/**
	 * Timestamp  started
	 */
	private Long startTimeMillis;

	/**
	 * Timestamp  completed
	 */
	private Long endTimeMillis;

	/**
	 * Additional metadata for the result
	 */
	private java.util.Map<String, Object> metadata = new java.util.HashMap<>();

	/**
	 * Get execution duration in milliseconds
	 */
	@JsonIgnore
	public Long getDurationMillis() {
		if (startTimeMillis != null && endTimeMillis != null) {
			return endTimeMillis - startTimeMillis;
		}
		return null;
	}
}

