import type { InvestmentRound, RuleType } from "@/lib/types/round";
import { apiGet, apiPost } from "./client";
import { toBackendRuleType, toFrontRuleType, type BackendRuleType } from "./mappers";
import { isApiClientError } from "./types";

interface BackendRoundRule {
  ruleId: number;
  ruleType: BackendRuleType;
  thresholdValue: number;
}

interface BackendRoundWallet {
  walletId: number;
  exchangeId: number;
}

interface BackendRound {
  roundId: number;
  userId: number;
  roundNumber: number;
  status: "ACTIVE" | "BANKRUPT" | "ENDED";
  initialSeed: number;
  emergencyFundingLimit: number;
  emergencyChargeCount: number;
  startedAt: string;
  endedAt: string | null;
  rules: BackendRoundRule[];
  wallets: BackendRoundWallet[];
}

interface StartRoundSeedRequest {
  exchangeId: number;
  amount: number;
}

interface StartRoundRuleRequest {
  ruleType: BackendRuleType;
  thresholdValue: number;
}

interface StartRoundRequestBody {
  userId: number;
  seeds: StartRoundSeedRequest[];
  emergencyFundingLimit: number;
  rules: StartRoundRuleRequest[];
}

export interface CreateRoundParams {
  userId: number;
  initialSeed: number;
  emergencyFundingLimit: number;
  rules: Array<{
    ruleType: RuleType;
    thresholdValue: number;
  }>;
}

interface ChargeEmergencyFundingRequestBody {
  userId: number;
  amount: number;
  idempotencyKey: string;
}

interface ChargeEmergencyFundingResponse {
  roundId: number;
  chargedAmount: number;
  remainingChargeCount: number;
}

export interface ChargeEmergencyFundingParams {
  roundId: number;
  userId: number;
  amount: number;
  idempotencyKey: string;
}

function mapRound(data: BackendRound): InvestmentRound {
  return {
    roundId: data.roundId,
    userId: data.userId,
    roundNumber: data.roundNumber,
    initialSeed: Number(data.initialSeed),
    emergencyFundingLimit: Number(data.emergencyFundingLimit),
    emergencyChargeCount: data.emergencyChargeCount,
    status: data.status,
    rules: (data.rules ?? []).map((rule) => ({
      ruleId: rule.ruleId,
      ruleType: toFrontRuleType(rule.ruleType),
      thresholdValue: Number(rule.thresholdValue),
    })),
    wallets: (data.wallets ?? []).map((w) => ({
      walletId: w.walletId,
      exchangeId: w.exchangeId,
    })),
    startedAt: data.startedAt,
    endedAt: data.endedAt,
  };
}

function toStartRoundBody(params: CreateRoundParams): StartRoundRequestBody {
  return {
    userId: params.userId,
    // 시드머니는 업비트에만 넣을 수 있다. 나머지 거래소는 0원으로 보내 송금받을 지갑만 만든다.
    seeds: [
      { exchangeId: 1, amount: params.initialSeed },
      { exchangeId: 2, amount: 0 },
      { exchangeId: 3, amount: 0 },
    ],
    emergencyFundingLimit: params.emergencyFundingLimit,
    rules: params.rules.map((rule) => ({
      ruleType: toBackendRuleType(rule.ruleType),
      thresholdValue: rule.thresholdValue,
    })),
  };
}

export async function createRound(params: CreateRoundParams): Promise<InvestmentRound> {
  const data = await apiPost<BackendRound>("/api/rounds", toStartRoundBody(params));
  return mapRound(data);
}

const ROUND_NOT_ACTIVE = "ROUND_NOT_ACTIVE";

export async function fetchActiveRound(userId: number): Promise<InvestmentRound | null> {
  try {
    const data = await apiGet<BackendRound>("/api/rounds/active", { userId });
    return mapRound(data);
  } catch (error) {
    if (isApiClientError(error) && error.code === ROUND_NOT_ACTIVE) {
      return null;
    }
    throw error;
  }
}

export async function fetchTotalRoundCount(userId: number): Promise<number> {
  const data = await apiGet<{ totalRoundCount: number }>("/api/rounds/summary", { userId });
  return data.totalRoundCount;
}

export async function chargeEmergencyFunding(
  params: ChargeEmergencyFundingParams,
): Promise<ChargeEmergencyFundingResponse> {
  const requestBody: ChargeEmergencyFundingRequestBody = {
    userId: params.userId,
    amount: params.amount,
    idempotencyKey: params.idempotencyKey,
  };

  return apiPost<ChargeEmergencyFundingResponse>(
    `/api/rounds/${params.roundId}/emergency-funding`,
    requestBody,
  );
}

export async function endRound(roundId: number, userId: number): Promise<void> {
  await apiPost(`/api/rounds/${roundId}/end`, { userId });
}

export function createIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

