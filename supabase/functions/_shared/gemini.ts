/**
 * central gemini.ts - Centralized, production-grade Gemini AI client for Supabase Edge Functions.
 * Implements low-temperature JSON requirements, timeouts, exponential backoff retries, 
 * malformed JSON scrubbing, and structured schema validations.
 */

export const GEMINI_1_5_FLASH = "gemini-3.5-flash";
export const GEMINI_1_5_PRO = "gemini-3.1-flash-lite";

export interface GeminiRequestOptions {
  model?: string;
  temperature?: number;
  topP?: number;
  responseMimeType?: string;
  timeoutMs?: number;
  maxRetries?: number;
  systemPrompt?: string;
  zodSchema?: { safeParse: (data: any) => { success: boolean; data?: any; error?: any } };
}

/**
 * Standard utility to clean up common LLM JSON output flaws (markdown formatting, comments, trailing commas, etc.)
 */
export function scrubMalformedJson(text: string): string {
  let cleaned = text.trim();
  
  // 1. Remove Markdown code block wrappers
  cleaned = cleaned.replace(/^```json\s*/i, "");
  cleaned = cleaned.replace(/^```\s*/, "");
  cleaned = cleaned.replace(/```$/, "");
  cleaned = cleaned.trim();

  // 2. Remove trailing commas before closing brackets/braces (e.g. [1, 2, ] -> [1, 2])
  cleaned = cleaned.replace(/,(\s*[\]}])/g, "$1");
  
  return cleaned;
}

/**
 * Executes a call to the Google Gemini API with robust retries, timeouts, and JSON scrubbing.
 */
export async function callGemini(
  apiKey: string,
  contents: any[],
  options: GeminiRequestOptions = {}
): Promise<any> {
  const model = options.model || GEMINI_1_5_FLASH;
  const temperature = options.temperature !== undefined ? options.temperature : 0.1;
  const topP = options.topP !== undefined ? options.topP : 0.8;
  const responseMimeType = options.responseMimeType || "application/json";
  const timeoutMs = options.timeoutMs || 30000;
  const maxRetries = options.maxRetries || 3;
  
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

  // Assemble Payload
  const payload: any = {
    contents: contents,
    generationConfig: {
      temperature,
      topP,
      responseMimeType
    }
  };

  // Prepend System Instructions if supplied
  if (options.systemPrompt) {
    payload.systemInstruction = {
      parts: [{ text: options.systemPrompt }]
    };
  }

  let attempt = 0;
  let lastError: Error | null = null;

  while (attempt < maxRetries) {
    attempt++;
    const abortController = new AbortController();
    const timeoutId = setTimeout(() => abortController.abort(), timeoutMs);

    try {
      console.log(`[GeminiClient] Call model ${model} - Attempt ${attempt}/${maxRetries}`);
      
      const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
        signal: abortController.signal
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Gemini API returned error code ${response.status}: ${errorText}`);
      }

      const responseBody = await response.json();
      const rawText = responseBody.candidates?.[0]?.content?.parts?.[0]?.text;
      
      if (!rawText) {
        throw new Error("Gemini response is empty or missing structured text parts.");
      }

      // If expecting JSON, scrub and validate
      if (responseMimeType === "application/json") {
        const scrubbed = scrubMalformedJson(rawText);
        let parsed: any;
        
        try {
          parsed = JSON.parse(scrubbed);
        } catch (jsonErr: any) {
          throw new Error(`Failed to parse response as JSON: ${jsonErr.message}. Raw text: ${rawText}`);
        }

        // Validate Zod Schema if injected using the centralized validation and repair rules
        if (options.zodSchema) {
          const { validateAndRepairResponse } = await import("./validation.ts");
          return validateAndRepairResponse(options.zodSchema as any, parsed, {
            minConfidenceThreshold: 0.60,
            tagLowConfidenceItems: true
          });
        }

        return parsed;
      }

      return rawText;

    } catch (err: any) {
      clearTimeout(timeoutId);
      lastError = err;
      
      const isTimeout = err.name === "AbortError";
      console.warn(
        `[GeminiClient] Attempt ${attempt} failed: ${isTimeout ? "Timeout" : err.message}`
      );

      if (attempt < maxRetries) {
        // Exponential Backoff with Jitter
        const backoffDelay = Math.pow(2, attempt) * 1000 + Math.random() * 500;
        console.log(`[GeminiClient] Retrying in ${Math.round(backoffDelay)}ms...`);
        await new Promise((resolve) => setTimeout(resolve, backoffDelay));
      }
    }
  }

  throw new Error(`All Gemini API attempts failed. Last error: ${lastError?.message}`);
}
